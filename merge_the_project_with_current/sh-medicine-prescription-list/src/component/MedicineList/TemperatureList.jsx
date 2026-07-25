import React, { useRef, useState } from "react";

export default function TemperatureList() {
  const canvasRef = useRef(null);
  const [points, setPoints] = useState([]);

  const handleCanvasClick = (e) => {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();

    
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const newPoints = [...points, { x, y }];
    setPoints(newPoints);

    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "red";
    ctx.beginPath();
    ctx.arc(x, y, 3, 0, Math.PI * 2);
    ctx.fill();

    if (newPoints.length > 1) {
      const prev = newPoints[newPoints.length - 2];
      ctx.strokeStyle = "blue";
      ctx.beginPath();
      ctx.moveTo(prev.x, prev.y);
      ctx.lineTo(x, y);
      ctx.stroke();
    }
  };
  return (
    <canvas
      ref={canvasRef}
      width={600}
      height={400}
      style={{ border: "1px solid black" }}
      onClick={handleCanvasClick}
    />
  );
}
