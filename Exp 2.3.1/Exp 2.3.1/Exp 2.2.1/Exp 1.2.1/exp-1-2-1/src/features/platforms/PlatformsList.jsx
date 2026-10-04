import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectAllPlatforms, platformStatusUpdated } from './platformsSlice';

export function PlatformsList() {
  const dispatch = useDispatch();
  const platforms = useSelector(selectAllPlatforms);

  const toggleStatus = (id, currentStatus) => {
    const nextStatus = currentStatus === 'Active' ? 'Maintenance' : 'Active';
    dispatch(platformStatusUpdated({ id, status: nextStatus }));
  };

  return (
    <div className="card">
      <h2>Platform Data</h2>
      <div className="platform-grid">
        {platforms.map((platform) => (
          <div key={platform.id} className="platform-item">
            <strong>{platform.name}</strong>
            <span className={`badge ${platform.status.toLowerCase()}`}>
              {platform.status}
            </span>
            <button
              className="btn-secondary"
              onClick={() => toggleStatus(platform.id, platform.status)}
            >
              Toggle Status
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
