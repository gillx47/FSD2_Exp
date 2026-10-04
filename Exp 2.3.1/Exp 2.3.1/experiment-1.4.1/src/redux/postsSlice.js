import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  posts: [
    { id: '1', title: '🚀 Launch Announcement', start: '2026-06-10T10:00:00' },
    { id: '2', title: '📊 Weekly Analytics Report', start: '2026-06-15T14:30:00' },
  ],
};

const postsSlice = createSlice({
  name: 'posts',
  initialState,
  reducers: {
    addPost: (state, action) => {
      state.posts.push(action.payload);
    },
    updatePostDate: (state, action) => {
      const { id, start } = action.payload;
      const post = state.posts.find((p) => p.id === id);
      if (post) {
        post.start = start;
      }
    },
  },
});

export const { addPost, updatePostDate } = postsSlice.actions;
export default postsSlice.reducer;
