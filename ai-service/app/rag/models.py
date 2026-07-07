"""
RAG Models - Data models for the RAG pipeline
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Literal, Dict, Any
from datetime import datetime


class KnowledgeDocument(BaseModel):
    """A chunk of indexed knowledge"""
    id: str
    source: Literal[
        "inventory", "orders", "products", "users", 
        "teams", "policies", "faq", "manual"
    ]
    source_id: str  # Reference to original entity
    content: str
    metadata: Dict[str, Any] = Field(default_factory=dict)
    embedding: Optional[List[float]] = None
    chunk_index: int = 0
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)


class RetrievedDocument(BaseModel):
    """A document retrieved from the knowledge base"""
    document: KnowledgeDocument
    relevance_score: float = Field(ge=0, le=1)
    rank: int


class RAGRequest(BaseModel):
    """Standard RAG request"""
    query: str
    team_id: str
    user_id: str
    sources: Optional[List[str]] = None
    max_documents: int = Field(default=5, ge=1, le=20)
    conversation_id: Optional[str] = None


class StandardizedAIResponse(BaseModel):
    """Standardized AI response format"""
    # Core answer
    answer: str
    reasoning_summary: str
    
    # Knowledge attribution
    referenced_knowledge: List[Dict[str, Any]] = Field(default_factory=list)
    
    # Confidence
    confidence: Dict[str, Any] = Field(default_factory=dict)
    
    # Actions
    suggested_actions: List[Dict[str, Any]] = Field(default_factory=list)
    
    # Metadata
    metadata: Dict[str, Any] = Field(default_factory=dict)


class SourceAttribution(BaseModel):
    """Citation format for responses"""
    document_id: str
    source: str
    source_id: str
    title: str
    excerpt: str
    relevance_score: float
    url: Optional[str] = None
    category: Optional[str] = None
    last_updated: Optional[str] = None


class DocumentProvenance(BaseModel):
    """Document source tracking"""
    document_id: str
    source: str
    source_id: str
    original_created_at: str
    original_updated_at: str
    indexed_at: str
    indexed_by: str
    index_version: int


class ConversationMessage(BaseModel):
    """A message in the conversation history"""
    role: Literal["user", "assistant"]
    content: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class RetrievalConfig(BaseModel):
    """Configuration for retrieval"""
    max_documents: int = 10
    min_relevance_score: float = 0.5
    rerank_top_k: int = 20
    sources: Optional[List[str]] = None
    date_from: Optional[str] = None
    date_to: Optional[str] = None


class QualityMetrics(BaseModel):
    """Quality assessment for a document"""
    completeness: float = Field(ge=0, le=1)
    accuracy: float = Field(ge=0, le=1)
    freshness: float = Field(ge=0, le=1)
    relevance: float = Field(ge=0, le=1)
    overall: float = Field(ge=0, le=1)


class ValidationResult(BaseModel):
    """Result of document validation"""
    valid: bool
    errors: List[str] = Field(default_factory=list)
    warnings: List[str] = Field(default_factory=list)
    quality_score: Optional[QualityMetrics] = None


class IndexingJob(BaseModel):
    """Status of an indexing job"""
    job_id: str
    source: str
    status: Literal["pending", "processing", "completed", "failed"]
    documents_processed: int = 0
    documents_total: int = 0
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    error: Optional[str] = None
