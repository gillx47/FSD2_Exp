import React, { useState } from 'react';
import { PLATFORMS } from './platformConfig';
import './PostComposer.css';

function PostComposer() {
  const [text, setText] = useState('');
  const [selectedPlatforms, setSelectedPlatforms] = useState(['twitter', 'linkedin']);
  const [hasMedia, setHasMedia] = useState(false);

  const togglePlatform = (id) => {
    if (selectedPlatforms.includes(id)) {
      setSelectedPlatforms(selectedPlatforms.filter((p) => p !== id));
    } else {
      setSelectedPlatforms([...selectedPlatforms, id]);
    }
  };

  const hashtagCount = (text.match(/#\w+/g) || []).length;

  const getValidationStatus = (platformId) => {
    const config = PLATFORMS[platformId];
    const errors = [];

    if (text.length > config.maxChars) {
      errors.push(`Exceeds maximum limit by ${text.length - config.maxChars} chars.`);
    }
    if (hashtagCount > config.maxHashtags) {
      errors.push(`Hashtag cap reached (${hashtagCount}/${config.maxHashtags}).`);
    }
    if (config.requiresMedia && !hasMedia) {
      errors.push('Media upload required for publication.');
    }

    return errors;
  };

  return (
    <div className="custom-app-wrapper">
      <div className="composer-card">
        <header className="composer-header">
          <span className="badge">Experiment 1.1.1</span>
          <h1>Social Post Studio</h1>
          <p>Compose, validate, and publish across channels seamlessly.</p>
        </header>

        {/* Target Channels Selection */}
        <section className="section-block">
          <label className="section-title">Target Channels</label>
          <div className="platform-chips">
            {Object.values(PLATFORMS).map((p) => {
              const isSelected = selectedPlatforms.includes(p.id);
              return (
                <button
                  key={p.id}
                  className={`chip chip-${p.id} ${isSelected ? 'active' : ''}`}
                  onClick={() => togglePlatform(p.id)}
                  type="button"
                >
                  <span className="chip-indicator"></span>
                  {p.name}
                </button>
              );
            })}
          </div>
        </section>

        {/* Text Input Area */}
        <section className="section-block">
          <div className="label-row">
            <label className="section-title">Content Editor</label>
            <span className="tag-count">Hashtags detected: {hashtagCount}</span>
          </div>
          <div className="editor-wrapper">
            <textarea
              className="styled-textarea"
              rows="5"
              placeholder="Craft your post content here... Add tags like #react #webdev"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <div className="editor-footer">
              <span className="character-tracker">
                Total Characters: <strong>{text.length}</strong>
              </span>
            </div>
          </div>
        </section>

        {/* Media Option */}
        <section className="section-block">
          <label className="custom-checkbox">
            <input
              type="checkbox"
              checked={hasMedia}
              onChange={(e) => setHasMedia(e.target.checked)}
            />
            <span className="checkbox-box"></span>
            <span className="checkbox-label">Attach Image / Video Asset</span>
          </label>
        </section>

        {/* Platform Compliance Monitor */}
        <section className="section-block">
          <label className="section-title">Platform Compliance Monitor</label>
          {selectedPlatforms.length === 0 ? (
            <div className="empty-state">Select at least one platform above to inspect constraints.</div>
          ) : (
            <div className="compliance-grid">
              {selectedPlatforms.map((pId) => {
                const platform = PLATFORMS[pId];
                const errors = getValidationStatus(pId);
                const isInvalid = errors.length > 0;
                const progressPct = Math.min(100, Math.round((text.length / platform.maxChars) * 100));

                return (
                  <div
                    key={pId}
                    className={`status-card platform-border-${pId} ${isInvalid ? 'status-error' : 'status-ok'}`}
                  >
                    <div className="status-head">
                      <span className="platform-tag">{platform.name}</span>
                      <span className="char-badge">
                        {text.length} / {platform.maxChars}
                      </span>
                    </div>

                    {/* Progress Bar */}
                    <div className="progress-bar-bg">
                      <div
                        className={`progress-bar-fill ${isInvalid && text.length > platform.maxChars ? 'overfill' : ''}`}
                        style={{ width: `${progressPct}%` }}
                      ></div>
                    </div>

                    {isInvalid ? (
                      <ul className="alert-list">
                        {errors.map((err, idx) => (
                          <li key={idx}>✕ {err}</li>
                        ))}
                      </ul>
                    ) : (
                      <div className="ok-message">✓ Compliant & ready to publish</div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

export default PostComposer;
