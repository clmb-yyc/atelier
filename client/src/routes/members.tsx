import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute('/members')({ component: Members })

function Members() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Members</h1>
      <p className="mt-4 text-lg">
        This is the page for managing members.
      </p>
    </div>
  )
}
