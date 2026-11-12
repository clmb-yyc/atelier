import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/bookings-classes')({ component: BookingsClasses })

function BookingsClasses() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Bookings &amp; Classes</h1>
      <p className="mt-4 text-lg">
        Manage bookings and classes.
      </p>
    </div>
  )
}
