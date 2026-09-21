import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { updatePostDate } from '../redux/postsSlice';
import PostModal from './PostModal';

const PostCalendar = () => {
  const dispatch = useDispatch();
  const posts = useSelector((state) => state.posts.posts);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState('');

  const handleDateSelect = (selectInfo) => {
    setSelectedDate(selectInfo.startStr);
    setIsModalOpen(true);
  };

  const handleEventDrop = (dropInfo) => {
    dispatch(
      updatePostDate({
        id: dropInfo.event.id,
        start: dropInfo.event.startStr,
      })
    );
  };

  return (
    <div className="max-w-5xl mx-auto p-6 bg-white shadow-lg rounded-xl mt-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Content Scheduling Calendar</h2>
        <button
          onClick={() => {
            setSelectedDate(new Date().toISOString().slice(0, 16));
            setIsModalOpen(true);
          }}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          + Add New Post
        </button>
      </div>

      <FullCalendar
        plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
        initialView="dayGridMonth"
        headerToolbar={{
          left: 'prev,next today',
          center: 'title',
          right: 'dayGridMonth,timeGridWeek,timeGridDay',
        }}
        editable={true}
        selectable={true}
        select={handleDateSelect}
        events={posts}
        eventDrop={handleEventDrop}
        height="600px"
      />

      <PostModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        selectedDate={selectedDate}
      />
    </div>
  );
};

export default PostCalendar;
