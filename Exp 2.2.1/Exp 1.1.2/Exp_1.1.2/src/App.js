import React, { useState, useEffect } from 'react';
import './App.css';

const STORAGE_KEY = 'app_post_drafts';

export default function App() {
  const [drafts, setDrafts] = useState([]);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [editingId, setEditingId] = useState(null);

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // 1. Safely load initial drafts on mount
  useEffect(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          setDrafts(parsed);
        }
      }
    } catch (err) {
      console.error('Failed to read from localStorage:', err);
    }
  }, []);

  // 2. Save / Update Draft Handler
  const handleSaveDraft = (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');
    setMessage('');

    // Validation
    if (!title.trim() || !content.trim()) {
      setErrorMessage('⚠️ Both Title and Content are required!');
      return;
    }

    setLoading(true);

    try {
      let updatedDrafts = [];

      if (editingId) {
        // Edit flow
        updatedDrafts = drafts.map((d) =>
          d.id === editingId
            ? { ...d, title: title.trim(), content: content.trim(), updatedAt: new Date().toISOString() }
            : d
        );
        setEditingId(null);
        setMessage('✨ Draft updated successfully!');
      } else {
        // Create flow
        const newDraft = {
          id: Date.now().toString(),
          title: title.trim(),
          content: content.trim(),
          updatedAt: new Date().toISOString(),
        };
        updatedDrafts = [newDraft, ...(Array.isArray(drafts) ? drafts : [])];
        setMessage('🎉 Draft saved to list below!');
      }

      // Synchronize React state
      setDrafts(updatedDrafts);

      // Persist to browser LocalStorage
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updatedDrafts));

      // Clear Form Inputs
      setTitle('');
      setContent('');
    } catch (err) {
      console.error('Error saving draft:', err);
      setErrorMessage(`Failed to save draft: ${err.message || 'Storage error'}`);
    } finally {
      setLoading(false);
    }
  };

  // Populate form for editing
  const handleEdit = (draft) => {
    setEditingId(draft.id);
    setTitle(draft.title);
    setContent(draft.content);
    setErrorMessage('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Delete draft
  const handleDelete = (id) => {
    try {
      const updated = drafts.filter((d) => d.id !== id);
      setDrafts(updated);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));

      if (editingId === id) {
        setEditingId(null);
        setTitle('');
        setContent('');
      }
      setMessage('🗑️ Draft removed');
    } catch (err) {
      console.error('Failed to delete:', err);
    }
  };

  return (
    <div style={styles.container}>
      <h2>📝 Draft Management System</h2>

      {/* Messages */}
      {message && <div style={styles.successBanner}>{message}</div>}
      {errorMessage && <div style={styles.errorBanner}>{errorMessage}</div>}

      {/* Form Card */}
      <div style={styles.card}>
        <h3>{editingId ? '✏️ Edit Draft' : '➕ Create New Draft'}</h3>

        <form onSubmit={handleSaveDraft} style={styles.form}>
          <input
            type="text"
            placeholder="Post Title..."
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            style={styles.input}
          />

          <textarea
            placeholder="Write post content..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows="5"
            style={styles.textarea}
          />

          <div style={styles.buttonGroup}>
            <button
              type="submit"
              disabled={loading}
              style={styles.primaryBtn}
            >
              {editingId ? 'Update Draft' : 'Save as Draft'}
            </button>

            {editingId && (
              <button
                type="button"
                onClick={() => {
                  setEditingId(null);
                  setTitle('');
                  setContent('');
                }}
                style={styles.secondaryBtn}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      <hr style={{ margin: '30px 0', borderColor: '#eee' }} />

      {/* Saved Drafts Display Section */}
      <h3>Saved Drafts ({drafts.length})</h3>

      {drafts.length === 0 ? (
        <div style={styles.emptyState}>
          No drafts saved yet. Type a title and content above, then click <strong>Save as Draft</strong>.
        </div>
      ) : (
        <div style={styles.list}>
          {drafts.map((draft) => (
            <div key={draft.id} style={styles.draftCard}>
              <div style={styles.draftHeader}>
                <h4 style={{ margin: 0 }}>{draft.title}</h4>
                <span style={styles.timeBadge}>
                  {new Date(draft.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>
              <p style={styles.draftBody}>{draft.content}</p>
              <div style={styles.actionRow}>
                <button onClick={() => handleEdit(draft)} style={styles.actionBtn}>
                  ✏️ Edit
                </button>
                <button onClick={() => handleDelete(draft.id)} style={styles.deleteBtn}>
                  🗑️ Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const styles = {
  container: { maxWidth: '640px', margin: '30px auto', fontFamily: 'system-ui, sans-serif', padding: '0 20px', color: '#333' },
  card: { background: '#fff', border: '1px solid #e0e0e0', borderRadius: '10px', padding: '20px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' },
  form: { display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '15px' },
  input: { padding: '10px 14px', borderRadius: '6px', border: '1px solid #ccc', fontSize: '15px' },
  textarea: { padding: '10px 14px', borderRadius: '6px', border: '1px solid #ccc', fontSize: '15px', resize: 'vertical' },
  buttonGroup: { display: 'flex', gap: '10px' },
  primaryBtn: { padding: '10px 18px', background: '#0066cc', color: '#fff', border: 'none', borderRadius: '6px', fontWeight: 'bold', cursor: 'pointer' },
  secondaryBtn: { padding: '10px 18px', background: '#e0e0e0', color: '#333', border: 'none', borderRadius: '6px', cursor: 'pointer' },
  successBanner: { padding: '10px', background: '#d4edda', color: '#155724', borderRadius: '6px', marginBottom: '15px' },
  errorBanner: { padding: '10px', background: '#f8d7da', color: '#721c24', borderRadius: '6px', marginBottom: '15px' },
  list: { display: 'flex', flexDirection: 'column', gap: '12px' },
  draftCard: { background: '#f9fafb', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '16px' },
  draftHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  timeBadge: { fontSize: '12px', color: '#6b7280' },
  draftBody: { margin: '10px 0 14px 0', whiteSpace: 'pre-wrap', color: '#374151' },
  actionRow: { display: 'flex', gap: '8px' },
  actionBtn: { padding: '6px 12px', background: '#fff', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer', fontSize: '13px' },
  deleteBtn: { padding: '6px 12px', background: '#fee2e2', border: '1px solid #fca5a5', color: '#991b1b', borderRadius: '4px', cursor: 'pointer', fontSize: '13px' },
  emptyState: { textAlign: 'center', padding: '30px', background: '#f9fafb', borderRadius: '8px', color: '#6b7280' },
};
