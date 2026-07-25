import React, { useState, useEffect } from 'react';

const DateRangeCalendar = ({ onRangeChange }) => {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [dateRange, setDateRange] = useState([]);

  const getDatesInRange = (start, end) => {
    const dates = [];
    let current = new Date(start);
    const endDate = new Date(end);

    while (current <= endDate) {
      dates.push(new Date(current).toISOString().split('T')[0]);
      current.setDate(current.getDate() + 1);
    }

    return dates;
  };

  useEffect(() => {
    if (startDate && endDate && new Date(startDate) <= new Date(endDate)) {
      const range = getDatesInRange(startDate, endDate);
      setDateRange(range);
      if (onRangeChange) {
        onRangeChange(range);
      }
    } else {
      setDateRange([]);
      if (onRangeChange) {
        onRangeChange([]);
      }
    }
  }, [startDate, endDate]);

  return (
    <div className="flex flex-col gap-4 p-4 max-w-sm">
      <label>
        Start Date:
        <input
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          className="border p-1 rounded"
        />
      </label>
      <label>
        End Date:
        <input
          type="date"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          className="border p-1 rounded"
        />
      </label>
      <div className="text-sm mt-2">
        <strong>Selected Range:</strong>
        <ul className="list-disc pl-4">
          {dateRange.map((date) => (
            <li key={date}>{date}</li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default DateRangeCalendar;