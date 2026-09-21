import { createSlice, createEntityAdapter } from '@reduxjs/toolkit';

const platformsAdapter = createEntityAdapter();

const initialPlatforms = [
  { id: 'plt1', name: 'Web Portal', status: 'Active' },
  { id: 'plt2', name: 'Mobile App', status: 'Active' },
  { id: 'plt3', name: 'Admin Dashboard', status: 'Maintenance' },
];

const platformsSlice = createSlice({
  name: 'platforms',
  initialState: platformsAdapter.setAll(platformsAdapter.getInitialState(), initialPlatforms),
  reducers: {
    platformStatusUpdated: (state, action) => {
      const { id, status } = action.payload;
      platformsAdapter.updateOne(state, { id, changes: { status } });
    },
  },
});

export const { platformStatusUpdated } = platformsSlice.actions;

export const { selectAll: selectAllPlatforms } = platformsAdapter.getSelectors(
  (state) => state.platforms
);

export default platformsSlice.reducer;
