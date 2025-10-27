# Troubleshooting: Agent A (Java + Maven)

## 1. Documents are not indexed
**Symptom:** "The docs folder was not found" or answers are always empty.  
**Check:**
- `paths.docs_dir` in `application.yaml` points to an existing folder.
- The folder contains `.md`, `.txt`, or `.pdf` files.
- After adding or modifying files, run the `:reload` command in the Agent console.

**Tip:** Keep the path relative to your project root (where `pom.xml` is located).

---

## 2. Embedding errors (401 / 429 / 500)
**Symptoms:** `Embedding error: 401`, `429`, or `500`.  
**Causes & Fixes:**
- **401 Unauthorized:** `OPENAI_API_KEY` is missing or invalid.
- **429 Too Many Requests:** You’ve hit the API rate limit. Reduce reload frequency.
- **500 Internal Server Error:** Temporary provider issue. Try again later.

---

## 3. “Insufficient sources found”
**Message:** `insufficient sources found (X < 3)`  
**Fix:** Add more distinct documentation files. The agent requires several independent sources to answer reliably.

---

## 4. Empty PDF parsing
**Symptom:** `Agent doesn’t cite PDF content`.  
**Reason:** The PDF is image-based (scanned, no text layer).  
**Fix:** Re-save it as a searchable PDF (after OCR), or provide a .md summary.

---

## 5. Lucene search doesn’t return expected results
**Tips:**  
Use quotes for exact phrases: "OutOfMemoryError Metaspace".  
Always :reload after editing documents.

---

## 6. Path issues on Windows
Avoid Cyrillic, emoji, or spaces in the project path (e.g., use C:\dev\Agent_A).
InvalidPathException usually means the path contains unsupported characters.