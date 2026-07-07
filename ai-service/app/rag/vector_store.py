"""
Vector Store - Simple file-based vector storage for development
"""

import os
import json
import numpy as np
from pathlib import Path
from typing import List, Optional, Dict, Any, Tuple
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


class VectorStore:
    """
    Simple file-based vector store for development.
    For production, replace with Qdrant, Pinecone, or Weaviate.
    """
    
    def __init__(self, storage_path: str = "./data/vectors"):
        self.storage_path = Path(storage_path)
        self.storage_path.mkdir(parents=True, exist_ok=True)
        
        self.vectors_file = self.storage_path / "vectors.json"
        self.metadata_file = self.storage_path / "metadata.json"
        
        self._vectors: Dict[str, np.ndarray] = {}
        self._metadata: Dict[str, Dict[str, Any]] = {}
        
        self._load()
    
    def _load(self):
        """Load vectors and metadata from disk"""
        try:
            if self.vectors_file.exists():
                with open(self.vectors_file, 'r') as f:
                    data = json.load(f)
                    self._vectors = {
                        k: np.array(v) for k, v in data.get("vectors", {}).items()
                    }
            
            if self.metadata_file.exists():
                with open(self.metadata_file, 'r') as f:
                    self._metadata = json.load(f)
                    
            logger.info(f"Loaded {len(self._vectors)} vectors from storage")
            
        except Exception as e:
            logger.warning(f"Failed to load vector store: {e}")
            self._vectors = {}
            self._metadata = {}
    
    def _save(self):
        """Save vectors and metadata to disk"""
        try:
            with open(self.vectors_file, 'w') as f:
                json.dump({
                    "vectors": {
                        k: v.tolist() for k, v in self._vectors.items()
                    },
                    "updated_at": datetime.utcnow().isoformat()
                }, f)
            
            with open(self.metadata_file, 'w') as f:
                json.dump(self._metadata, f, default=str)
                
        except Exception as e:
            logger.error(f"Failed to save vector store: {e}")
    
    def add(
        self, 
        doc_id: str, 
        embedding: List[float], 
        metadata: Dict[str, Any]
    ):
        """
        Add a document vector.
        
        Args:
            doc_id: Unique document ID
            embedding: Vector embedding
            metadata: Document metadata
        """
        self._vectors[doc_id] = np.array(embedding)
        self._metadata[doc_id] = {
            **metadata,
            "indexed_at": datetime.utcnow().isoformat()
        }
        self._save()
    
    def search(
        self, 
        query_embedding: List[float], 
        k: int = 5,
        filter_source: Optional[str] = None
    ) -> List[Tuple[str, float]]:
        """
        Search for k most similar documents.
        
        Args:
            query_embedding: Query vector
            k: Number of results to return
            filter_source: Optional source filter
            
        Returns:
            List of (doc_id, similarity_score) tuples
        """
        if not self._vectors:
            return []
        
        query_vec = np.array(query_embedding)
        query_norm = np.linalg.norm(query_vec)
        
        if query_norm == 0:
            return []
        
        similarities = []
        
        for doc_id, doc_vec in self._vectors.items():
            # Apply source filter if specified
            if filter_source:
                doc_metadata = self._metadata.get(doc_id, {})
                if doc_metadata.get("source") != filter_source:
                    continue
            
            # Compute cosine similarity
            doc_norm = np.linalg.norm(doc_vec)
            if doc_norm == 0:
                continue
                
            sim = np.dot(query_vec, doc_vec) / (query_norm * doc_norm)
            similarities.append((doc_id, float(sim)))
        
        # Sort by similarity descending
        similarities.sort(key=lambda x: x[1], reverse=True)
        return similarities[:k]
    
    def get(self, doc_id: str) -> Optional[Dict[str, Any]]:
        """Get document metadata by ID"""
        return self._metadata.get(doc_id)
    
    def get_vector(self, doc_id: str) -> Optional[List[float]]:
        """Get document vector by ID"""
        vec = self._vectors.get(doc_id)
        return vec.tolist() if vec is not None else None
    
    def delete(self, doc_id: str):
        """Delete a document"""
        if doc_id in self._vectors:
            del self._vectors[doc_id]
        if doc_id in self._metadata:
            del self._metadata[doc_id]
        self._save()
    
    def delete_by_source(self, source: str):
        """Delete all documents from a source"""
        to_delete = [
            doc_id for doc_id, meta in self._metadata.items()
            if meta.get("source") == source
        ]
        
        for doc_id in to_delete:
            self.delete(doc_id)
    
    def count(self) -> int:
        """Get total number of vectors"""
        return len(self._vectors)
    
    def count_by_source(self, source: str) -> int:
        """Get number of vectors for a source"""
        return sum(
            1 for meta in self._metadata.values()
            if meta.get("source") == source
        )
    
    def list_sources(self) -> List[str]:
        """List all unique sources"""
        sources = set()
        for meta in self._metadata.values():
            if source := meta.get("source"):
                sources.add(source)
        return sorted(list(sources))
    
    def get_stats(self) -> Dict[str, Any]:
        """Get vector store statistics"""
        sources = self.list_sources()
        return {
            "total_vectors": self.count(),
            "sources": {
                source: self.count_by_source(source) 
                for source in sources
            },
            "dimension": len(list(self._vectors.values())[0]) if self._vectors else 0,
            "storage_path": str(self.storage_path)
        }
    
    def clear(self):
        """Clear all vectors and metadata"""
        self._vectors = {}
        self._metadata = {}
        self._save()


# Singleton instance
_vector_store: Optional[VectorStore] = None


def get_vector_store() -> VectorStore:
    """Get singleton vector store instance"""
    global _vector_store
    if _vector_store is None:
        storage_path = os.getenv("VECTOR_STORE_PATH", "./data/vectors")
        _vector_store = VectorStore(storage_path)
    return _vector_store
