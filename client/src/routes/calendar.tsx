import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/calendar')({ component: Calendar })

function Calendar() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Calendar</h1>
      <p className="mt-4 text-lg">
        View the calendar.
      </p>
    </div>
  )
}
