"""
Main RAG Service - Orchestrates the RAG pipeline
"""

import json
import time
from typing import List, Optional, Dict, Any
from datetime import datetime

from app.rag.models import (
    RAGRequest,
    StandardizedAIResponse,
    RetrievedDocument,
    KnowledgeDocument,
    SourceAttribution
)
from app.rag.embedding import EmbeddingService, get_embedding_service
from app.rag.vector_store import VectorStore, get_vector_store
from app.rag.prompt_builder import PromptBuilder


class RAGService:
    """
    Main RAG orchestration service.
    Coordinates embedding, retrieval, prompt building, and response formatting.
    """
    
    def __init__(
        self,
        embedding_service: Optional[EmbeddingService] = None,
        vector_store: Optional[VectorStore] = None
    ):
        self.embedding_service = embedding_service or get_embedding_service()
        self.vector_store = vector_store or get_vector_store()
        self.prompt_builder = PromptBuilder()
    
    def query(self, request: RAGRequest) -> StandardizedAIResponse:
        """
        Process a RAG query.
        
        Args:
            request: RAG request with query and parameters
            
        Returns:
            Standardized AI response with citations
        """
        start_time = time.time()
        
        # Sanitize query
        query = self._sanitize_input(request.query)
        
        # Get conversation history
        history = None
        if request.conversation_id:
            history = self._get_conversation_history(request.conversation_id)
        
        # Embed query
        query_embedding = self.embedding_service.embed_query(query)
        
        # Retrieve relevant documents
        retrieved_docs = self._retrieve(
            query_embedding=query_embedding,
            sources=request.sources,
            max_docs=request.max_documents
        )
        
        # Build context
        context = {
            "team_id": request.team_id,
            "user_id": request.user_id
        }
        
        # Build prompt
        prompt = self.prompt_builder.build(
            query=query,
            retrieved_docs=retrieved_docs,
            conversation_history=history,
            context=context
        )
        
        # Generate response (placeholder - would call LLM)
        response_text = self._generate_response(prompt)
        
        # Calculate confidence
        confidence = self._calculate_confidence(retrieved_docs)
        
        # Format citations
        citations = self._format_citations(retrieved_docs)
        
        # Extract structured components
        answer, reasoning, suggestions = self._parse_response(response_text)
        
        # Add to conversation history
        if request.conversation_id:
            self._add_to_history(
                request.conversation_id,
                request.user_id,
                query,
                answer
            )
        
        processing_time = time.time() - start_time
        
        return StandardizedAIResponse(
            answer=answer,
            reasoning_summary=reasoning,
            referenced_knowledge=citations,
            confidence=confidence,
            suggested_actions=suggestions,
            metadata={
                "model": "gemini-1.5-pro",
                "tokens_used": len(prompt.split()) * 1.3,
                "processing_time_ms": int(processing_time * 1000),
                "documents_retrieved": len(retrieved_docs),
                "query": query
            }
        )
    
    def _sanitize_input(self, text: str) -> str:
        """Remove potential prompt injection patterns"""
        import re
        
        dangerous_patterns = [
            r"ignore previous instructions",
            r"disregard.*instructions",
            r"you are now.*different",
            r"forget.*instruction",
            r"new instructions:",
            r"override.*security",
            r"admin.*mode",
            r"developer.*mode",
            r"```system",
            r"```instructions",
        ]
        
        for pattern in dangerous_patterns:
            text = re.sub(pattern, "[FILTERED]", text, flags=re.IGNORECASE)
        
        return text.strip()
    
    def _retrieve(
        self,
        query_embedding: List[float],
        sources: Optional[List[str]] = None,
        max_docs: int = 5
    ) -> List[RetrievedDocument]:
        """Retrieve documents from vector store"""
        results = self.vector_store.search(
            query_embedding=query_embedding,
            k=max_docs * 2  # Get more for filtering
        )
        
        retrieved = []
        rank = 0
        
        for doc_id, score in results:
            metadata = self.vector_store.get(doc_id)
            if not metadata:
                continue
            
            # Filter by source if specified
            if sources and metadata.get("source") not in sources:
                continue
            
            doc = KnowledgeDocument(
                id=doc_id,
                source=metadata.get("source", "unknown"),
                source_id=metadata.get("source_id", ""),
                content=metadata.get("content", ""),
                metadata=metadata.get("metadata", {}),
                chunk_index=metadata.get("chunk_index", 0)
            )
            
            retrieved.append(RetrievedDocument(
                document=doc,
                relevance_score=score,
                rank=rank
            ))
            rank += 1
            
            if len(retrieved) >= max_docs:
                break
        
        return retrieved
    
    def _generate_response(self, prompt: str) -> str:
        """
        Generate response using LLM.
        This is a placeholder - actual implementation would call Gemini API.
        """
        # In production, this would call Gemini API
        # For now, return a structured placeholder
        return json.dumps({
            "answer": "Tôi đã tìm thấy thông tin liên quan trong cơ sở tri thức ORCA.",
            "reasoning": "Dựa trên các tài liệu được truy xuất, tôi có thể cung cấp thông tin này.",
            "suggestions": [
                {"label": "Xem chi tiết", "type": "navigate", "payload": {}}
            ]
        })
    
    def _calculate_confidence(
        self, 
        docs: List[RetrievedDocument]
    ) -> Dict[str, Any]:
        """Calculate response confidence based on retrieved documents"""
        if not docs:
            return {
                "score": 0.0,
                "level": "low",
                "reasons": ["No relevant documents found in knowledge base"]
            }
        
        avg_relevance = sum(d.relevance_score for d in docs) / len(docs)
        
        if avg_relevance > 0.8:
            level = "high"
            reasons = ["High relevance scores from knowledge base"]
        elif avg_relevance > 0.5:
            level = "medium"
            reasons = ["Moderate relevance from knowledge base"]
        else:
            level = "low"
            reasons = ["Low relevance scores, response may be uncertain"]
        
        return {
            "score": round(avg_relevance, 2),
            "level": level,
            "reasons": reasons
        }
    
    def _format_citations(
        self, 
        docs: List[RetrievedDocument]
    ) -> List[Dict[str, Any]]:
        """Format documents as citations"""
        citations = []
        
        for doc in docs:
            meta = doc.document.metadata or {}
            
            citations.append({
                "document_id": doc.document.id,
                "source": doc.document.source,
                "source_id": doc.document.source_id,
                "title": meta.get("title", "Unknown"),
                "category": meta.get("category", "General"),
                "excerpt": self._truncate_content(doc.document.content, 200),
                "relevance_score": doc.relevance_score,
                "url": self._generate_url(doc.document)
            })
        
        return citations
    
    def _truncate_content(self, content: str, max_length: int) -> str:
        """Truncate content to max length"""
        if len(content) <= max_length:
            return content
        return content[:max_length].rsplit(" ", 1)[0] + "..."
    
    def _generate_url(self, doc: KnowledgeDocument) -> str:
        """Generate URL to view source document"""
        source_url_map = {
            "inventory": f"/inventory/item/{doc.source_id}",
            "orders": f"/orders/{doc.source_id}",
            "products": f"/products/{doc.source_id}",
            "policies": f"/settings/policies",
            "faq": f"/faq",
            "manual": f"/manual"
        }
        return source_url_map.get(doc.source, "/")
    
    def _parse_response(
        self, 
        response_text: str
    ) -> tuple[str, str, List[Dict[str, Any]]]:
        """Parse LLM response into structured components"""
        try:
            data = json.loads(response_text)
            return (
                data.get("answer", "Không có câu trả lời."),
                data.get("reasoning", "Dựa trên thông tin có sẵn."),
                data.get("suggestions", [])
            )
        except json.JSONDecodeError:
            return (
                response_text,
                "Dựa trên thông tin được truy xuất.",
                []
            )
    
    def _get_conversation_history(
        self, 
        conversation_id: str
    ) -> Optional[List[Dict[str, str]]]:
        """Get conversation history from storage"""
        # Would implement conversation memory here
        return None
    
    def _add_to_history(
        self,
        conversation_id: str,
        user_id: str,
        query: str,
        response: str
    ):
        """Add message to conversation history"""
        # Would implement conversation memory here
        pass
    
    def index_document(
        self,
        doc_id: str,
        content: str,
        metadata: Dict[str, Any],
        source: str,
        source_id: str
    ):
        """
        Index a document for retrieval.
        
        Args:
            doc_id: Unique document ID
            content: Text content to index
            metadata: Document metadata
            source: Source type
            source_id: Original entity ID
        """
        # Generate embedding
        embedding = self.embedding_service.embed([content])[0]
        
        # Store in vector database
        self.vector_store.add(doc_id, embedding, {
            "content": content,
            "metadata": metadata,
            "source": source,
            "source_id": source_id
        })
    
    def remove_document(self, doc_id: str):
        """Remove a document from the index"""
        self.vector_store.delete(doc_id)
    
    def get_stats(self) -> Dict[str, Any]:
        """Get RAG service statistics"""
        vector_stats = self.vector_store.get_stats()
        return {
            "documents_indexed": vector_stats["total_vectors"],
            "sources": vector_stats["sources"],
            "embedding_dimension": self.embedding_service.dimension,
            "embedding_model": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
        }


# Singleton instance
_rag_service: Optional[RAGService] = None


def get_rag_service() -> RAGService:
    """Get singleton RAG service instance"""
    global _rag_service
    if _rag_service is None:
        _rag_service = RAGService()
    return _rag_service
