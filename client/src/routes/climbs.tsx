import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute('/')({ component: Climbs })

function Climbs() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Climbs</h1>
      <p className="mt-4 text-lg">
        This is the page for managing climbing routes.
      </p>
    </div>
  )
}
