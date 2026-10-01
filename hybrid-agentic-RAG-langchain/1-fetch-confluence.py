"""
Step 1: Fetch all Confluence pages and save them as JSON chunks.

For each space in CONFLUENCE_SPACE_KEYS this script:
  1. Lists every page in the space via the Confluence REST API.
  2. Strips HTML from the storage-format body to get plain text.
  3. Splits long pages into overlapping chunks (~1 500 chars each) at line
     boundaries so that embedding stays within model limits and retrieval
     can pinpoint the relevant section rather than an entire 10-page document.
  4. Saves each chunk as chunks/{page_id}_c{idx}.json.

A stable chunk_id ("{page_id}_c{idx}") is used as the retrieval key
throughout the pipeline so results can always link back to the source page.

Run once (or after a Confluence update) before building indexes in step 2.

Usage:
  uv run 1-fetch-confluence.py
"""

import json
import os
from pathlib import Path

from dotenv import load_dotenv

from utils.confluence import ConfluenceClient, PageChunk

load_dotenv()

CHUNKS_DIR = Path("chunks")
CHUNKS_DIR.mkdir(exist_ok=True)

SPACE_KEYS = [
    s.strip()
    for s in os.environ.get("CONFLUENCE_SPACE_KEYS", "").split(",")
    if s.strip()
]


def process_page(client: ConfluenceClient, raw: dict, space_key: str) -> list[PageChunk]:
    """Convert one raw Confluence page dict into a list of PageChunk objects."""
    page_id = raw["id"]
    title = raw["title"]
    space_name = raw.get("space", {}).get("name", space_key)
    last_modified = raw.get("version", {}).get("when", "")
    html = raw.get("body", {}).get("storage", {}).get("value", "")
    text = ConfluenceClient.html_to_text(html)
    url = client.page_url(space_key, page_id)

    if not text.strip():
        return []

    raw_chunks = ConfluenceClient.chunk(text)
    return [
        PageChunk(
            chunk_id=f"{page_id}_c{idx}",
            page_id=page_id,
            title=title,
            space_key=space_key,
            space_name=space_name,
            url=url,
            text=chunk_text,
            chunk_idx=idx,
            last_modified=last_modified,
        )
        for idx, chunk_text in enumerate(raw_chunks)
    ]


def main() -> None:
    if not SPACE_KEYS:
        print("CONFLUENCE_SPACE_KEYS is not set in .env.\n")
        print("Listing available spaces so you can choose which to index:")
        client = ConfluenceClient()
        for s in client.list_spaces():
            print(f"  {s['key']:20s}  {s['name']}")
        print("\nSet CONFLUENCE_SPACE_KEYS=KEY1,KEY2 in .env and re-run.")
        return

    client = ConfluenceClient()
    total_chunks = 0

    for space_key in SPACE_KEYS:
        print(f"\nSpace: {space_key}")
        page_count = chunk_count = 0

        for raw_page in client.iter_pages(space_key):
            chunks = process_page(client, raw_page, space_key)
            for chunk in chunks:
                out = CHUNKS_DIR / f"{chunk.chunk_id}.json"
                out.write_text(
                    json.dumps(chunk.__dict__, ensure_ascii=False),
                    encoding="utf-8",
                )
            page_count += 1
            chunk_count += len(chunks)
            if page_count % 20 == 0:
                print(f"  {page_count} pages, {chunk_count} chunks so far...")

        total_chunks += chunk_count
        print(f"  Done: {page_count} pages → {chunk_count} chunks")

    print(f"\nTotal: {total_chunks} chunks saved to {CHUNKS_DIR}/")
    print("Next: run 2-build-index.py")


if __name__ == "__main__":
    main()
