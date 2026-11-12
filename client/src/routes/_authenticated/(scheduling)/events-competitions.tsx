import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(scheduling)/events-competitions')({ component: EventsCompetitions })

function EventsCompetitions() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Events &amp; Competitions</h1>
      <p className="mt-4 text-lg">
        Manage events and competitions.
      </p>
    </div>
  )
}
