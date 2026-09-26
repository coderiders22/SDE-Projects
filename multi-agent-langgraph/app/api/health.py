from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text

from app.db.session import get_db

router = APIRouter()


@router.get("/health", tags=["health"], summary="Health check")
def health(db: Session = Depends(get_db)):
    """Returns API status and database connectivity."""
    try:
        db.execute(text("SELECT 1"))
        db_status = "connected"
    except Exception as e:
        db_status = f"error: {e}"

    return {"status": "ok" if db_status == "connected" else "degraded", "database": db_status}
