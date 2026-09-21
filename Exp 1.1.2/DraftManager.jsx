import React, { useState, useEffect } from 'react';
import { FileText, Plus, Edit3, Trash2, Save, X, RefreshCw, Check, AlertCircle } from 'lucide-react';

export default function DraftManager() {
  const [drafts, setDrafts] = useState([]);
  const [formData, setFormData] = useState({ id: null, title: '', content: '' });
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [feedback, setFeedback] = useState(null);

  // Load saved drafts from localStorage on component mount
  useEffect(() => {
    const savedDrafts = localStorage.getItem('post_drafts');
    if (savedDrafts) {
      setDrafts(JSON.parse(savedDrafts));
    }
  }, []);

  // Sync state to localStorage whenever drafts change
  useEffect(() => {
    localStorage.setItem('post_drafts', JSON.stringify(drafts));
  }, [drafts]);

  const showNotification = (message, type = 'success') => {
    setFeedback({ message, type });
    setTimeout(() => setFeedback(null), 3000);
  };

  // Simulate an async API call latency
  const simulateAsyncOperation = (delay = 500) => {
    return new Promise((resolve) => setTimeout(resolve, delay));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.title.trim() || !formData.content.trim()) {
      showNotification('Please fill in both title and content!', 'error');
      return;
    }

    setIsLoading(true);
    await simulateAsyncOperation();

    if (isEditing) {
      setDrafts((prev) =>
        prev.map((d) => (d.id === formData.id ? { ...formData, updatedAt: new Date().toLocaleTimeString() } : d))
      );
      showNotification('Draft updated successfully!');
    } else {
      const newDraft = {
        ...formData,
        id: Date.now(),
        updatedAt: new Date().toLocaleTimeString(),
      };
      setDrafts((prev) => [newDraft, ...prev]);
      showNotification('Draft saved successfully!');
    }

    resetForm();
    setIsLoading(false);
  };

  const handleEdit = (draft) => {
    setFormData(draft);
    setIsEditing(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (id) => {
    setIsLoading(true);
    await simulateAsyncOperation();
    
    setDrafts((prev) => prev.filter((d) => d.id !== id));
    if (formData.id === id) resetForm();
    
    setIsLoading(false);
    showNotification('Draft deleted.', 'info');
  };

  const resetForm = () => {
    setFormData({ id: null, title: '', content: '' });
    setIsEditing(false);
  };

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <h1 style={styles.title}><FileText size={28} /> Draft Manager</h1>
        <p style={styles.subtitle}>Create, manage, and persist post drafts locally.</p>
      </header>

      {/* Alert Banner */}
      {feedback && (
        <div style={{ ...styles.alert, ...styles[feedback.type] }}>
          {feedback.type === 'error' ? <AlertCircle size={18} /> : <Check size={18} />}
          <span>{feedback.message}</span>
        </div>
      )}

      {/* Input Form */}
      <div style={styles.card}>
        <h2 style={styles.cardTitle}>
          {isEditing ? <Edit3 size={20} /> : <Plus size={20} />}
          {isEditing ? 'Edit Draft' : 'Create New Draft'}
        </h2>

        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            type="text"
            placeholder="Post Title..."
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            disabled={isLoading}
            style={styles.input}
          />

          <textarea
            placeholder="Write your draft content here..."
            value={formData.content}
            onChange={(e) => setFormData({ ...formData, content: e.target.value })}
            disabled={isLoading}
            rows={5}
            style={styles.textarea}
          />

          <div style={styles.buttonGroup}>
            <button type="submit" disabled={isLoading} style={styles.btnPrimary}>
              {isLoading ? (
                <>
                  <RefreshCw style={styles.spin} size={16} /> Saving...
                </>
              ) : (
                <>
                  <Save size={16} /> {isEditing ? 'Update Draft' : 'Save Draft'}
                </>
              )}
            </button>

            {isEditing && (
              <button type="button" onClick={resetForm} style={styles.btnSecondary}>
                <X size={16} /> Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Saved Drafts Display List */}
      <div style={styles.listSection}>
        <h3 style={styles.listTitle}>Saved Drafts ({drafts.length})</h3>

        {drafts.length === 0 ? (
          <div style={styles.emptyState}>
            <FileText size={48} color="#ccc" />
            <p>No drafts saved yet. Start writing above!</p>
          </div>
        ) : (
          <div style={styles.grid}>
            {drafts.map((draft) => (
              <div key={draft.id} style={styles.draftCard}>
                <div style={styles.draftHeader}>
                  <h4 style={styles.draftTitle}>{draft.title}</h4>
                  <span style={styles.timestamp}>{draft.updatedAt}</span>
                </div>
                <p style={styles.draftContent}>{draft.content}</p>
                <div style={styles.draftActions}>
                  <button onClick={() => handleEdit(draft)} style={styles.btnEdit}>
                    <Edit3 size={14} /> Edit
                  </button>
                  <button onClick={() => handleDelete(draft.id)} style={styles.btnDelete}>
                    <Trash2 size={14} /> Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// Inline Style Object
const styles = {
  container: { maxWidth: '700px', margin: '2rem auto', fontFamily: 'system-ui, sans-serif', padding: '0 1rem', color: '#333' },
  header: { textAlign: 'center', marginBottom: '1.5rem' },
  title: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', margin: 0, fontSize: '1.8rem' },
  subtitle: { color: '#666', marginTop: '4px' },
  card: { background: '#fff', border: '1px solid #e2e8f0', padding: '1.5rem', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)', marginBottom: '2rem' },
  cardTitle: { display: 'flex', alignItems: 'center', gap: '8px', margin: '0 0 1rem 0', fontSize: '1.2rem', color: '#1e293b' },
  form: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  input: { padding: '0.75rem', fontSize: '1rem', borderRadius: '6px', border: '1px solid #cbd5e1', outline: 'none' },
  textarea: { padding: '0.75rem', fontSize: '1rem', borderRadius: '6px', border: '1px solid #cbd5e1', outline: 'none', resize: 'vertical' },
  buttonGroup: { display: 'flex', gap: '0.5rem' },
  btnPrimary: { display: 'flex', alignItems: 'center', gap: '6px', padding: '0.6rem 1.2rem', backgroundColor: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 500 },
  btnSecondary: { display: 'flex', alignItems: 'center', gap: '6px', padding: '0.6rem 1.2rem', backgroundColor: '#64748b', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer' },
  alert: { display: 'flex', alignItems: 'center', gap: '8px', padding: '0.75rem 1rem', borderRadius: '6px', marginBottom: '1rem', color: '#fff', fontWeight: 500 },
  success: { backgroundColor: '#10b981' },
  error: { backgroundColor: '#ef4444' },
  info: { backgroundColor: '#0ea5e9' },
  listSection: { marginTop: '1rem' },
  listTitle: { fontSize: '1.3rem', marginBottom: '1rem', color: '#1e293b' },
  grid: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  draftCard: { padding: '1rem', border: '1px solid #e2e8f0', borderRadius: '8px', background: '#f8fafc' },
  draftHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' },
  draftTitle: { margin: 0, fontSize: '1.1rem', color: '#0f172a' },
  timestamp: { fontSize: '0.8rem', color: '#94a3b8' },
  draftContent: { color: '#475569', margin: '0 0 1rem 0', whiteSpace: 'pre-wrap' },
  draftActions: { display: 'flex', gap: '0.5rem' },
  btnEdit: { display: 'flex', alignItems: 'center', gap: '4px', padding: '0.4rem 0.8rem', backgroundColor: '#f59e0b', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' },
  btnDelete: { display: 'flex', alignItems: 'center', gap: '4px', padding: '0.4rem 0.8rem', backgroundColor: '#ef4444', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' },
  emptyState: { textAlign: 'center', padding: '3rem', border: '2px dashed #e2e8f0', borderRadius: '8px', color: '#94a3b8' },
  spin: { animation: 'spin 1s linear infinite' }
};
