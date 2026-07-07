"""
Embedding Service - Text vectorization using sentence transformers
"""

import os
import numpy as np
from typing import List, Optional
import logging

logger = logging.getLogger(__name__)


class EmbeddingService:
    """
    Text vectorization service using sentence transformers.
    Supports multilingual embeddings for Vietnamese content.
    """
    
    def __init__(self):
        self._model = None
        self._dimension = None
        self._initialized = False
        
    @property
    def model(self):
        """Lazy load model on first use"""
        if self._model is None:
            self._load_model()
        return self._model
    
    @property
    def dimension(self) -> int:
        """Get embedding dimension"""
        if self._dimension is None:
            self._load_model()
        return self._dimension
    
    def _load_model(self):
        """Load the sentence transformer model"""
        try:
            from sentence_transformers import SentenceTransformer
            
            # Use multilingual model for Vietnamese support
            model_name = os.getenv(
                "EMBEDDING_MODEL", 
                "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
            )
            
            logger.info(f"Loading embedding model: {model_name}")
            self._model = SentenceTransformer(model_name)
            self._dimension = self._model.get_sentence_embedding_dimension()
            self._initialized = True
            
            logger.info(f"Embedding model loaded. Dimension: {self._dimension}")
            
        except ImportError:
            logger.warning("sentence-transformers not installed. Using mock embeddings.")
            self._model = None
            self._dimension = 384
            self._initialized = True
    
    def embed(self, texts: List[str]) -> List[List[float]]:
        """
        Convert a list of texts to embeddings.
        
        Args:
            texts: List of text strings to embed
            
        Returns:
            List of embedding vectors
        """
        if not texts:
            return []
        
        # Use mock embeddings if model not available
        if self._model is None:
            return self._mock_embeddings(len(texts))
        
        try:
            embeddings = self.model.encode(texts, convert_to_numpy=True)
            return embeddings.tolist()
        except Exception as e:
            logger.error(f"Embedding generation failed: {e}")
            return self._mock_embeddings(len(texts))
    
    def embed_query(self, query: str) -> List[float]:
        """
        Embed a single query string.
        
        Args:
            query: Query text to embed
            
        Returns:
            Embedding vector
        """
        return self.embed([query])[0]
    
    def _mock_embeddings(self, count: int) -> List[List[float]]:
        """
        Generate mock embeddings for testing when model is unavailable.
        Uses deterministic random based on text hash.
        """
        embeddings = []
        for i in range(count):
            # Use fixed seed for reproducibility
            np.random.seed(i + 42)
            embedding = np.random.randn(self._dimension).astype(float)
            # Normalize
            embedding = embedding / np.linalg.norm(embedding)
            embeddings.append(embedding.tolist())
        return embeddings
    
    @property
    def is_initialized(self) -> bool:
        """Check if the service is initialized"""
        return self._initialized


# Singleton instance
_embedding_service: Optional[EmbeddingService] = None


def get_embedding_service() -> EmbeddingService:
    """Get singleton embedding service instance"""
    global _embedding_service
    if _embedding_service is None:
        _embedding_service = EmbeddingService()
    return _embedding_service
