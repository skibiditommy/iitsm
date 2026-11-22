import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, GitBranch, RefreshCw } from 'lucide-react';
import api from '../api';

interface Ticket { id: number; incidentId: string; title: string }
interface Dependent { id: number; path: string[]; activeTickets: Ticket[] }
interface ServiceImpact {
  id: number;
  name: string;
  activeTickets: Ticket[];
  dependents: Dependent[];
  affectedDependentCount: number;
}

function TicketLinks({ tickets }: { tickets: Ticket[] }) {
  return <ul className="flex flex-wrap gap-x-4 gap-y-1">
    {tickets.map(ticket => <li key={ticket.id}>
      <Link to={`/incidents/${ticket.id}`} className="text-sm text-indigo-700 hover:underline">
        {ticket.incidentId} · {ticket.title}
      </Link>
    </li>)}
  </ul>;
}

export default function ServiceImpactExplorer() {
  const [services, setServices] = useState<ServiceImpact[]>([]);
  const [selectedId, setSelectedId] = useState<number>();
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    api.get<ServiceImpact[]>('/analytics/service-impacts', { signal: controller.signal })
      .then(({ data }) => { if (!controller.signal.aborted) setServices(data); })
      .catch(() => { if (!controller.signal.aborted) setFailed(true); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [revision]);

  const selected = services.find(s => s.id === selectedId) ?? services[0];
  const suspects = services.filter(s => s.affectedDependentCount >= 2);
  return <section className="bg-white rounded-xl shadow p-6 mb-6">
    <div className="flex items-center justify-between gap-2">
      <h3 className="flex gap-2 items-center font-semibold"><GitBranch size={18} />Service impact explorer</h3>
      <button type="button" disabled={loading} onClick={() => { setLoading(true); setFailed(false); setRevision(r => r + 1); }}
        className="flex items-center gap-1 text-sm text-indigo-700 disabled:opacity-50"><RefreshCw size={15} />Refresh</button>
    </div>
    {loading && <p role="status" className="mt-3 text-sm text-gray-600">Loading dependencies…</p>}
    {failed && <p role="alert" className="mt-3 text-sm text-red-700">Could not load current service impacts. Use Refresh to retry.</p>}
    {!failed && !loading && (selected ? <>
      <h4 className="mt-5 font-medium">Possible shared causes</h4>
      <p className="text-sm text-gray-600">An upstream service is a candidate when at least two of its downstream services have active incidents.</p>
      {suspects.length ? <div className="flex flex-wrap gap-2 mt-2">
        {suspects.map(s => <button type="button" key={s.id} onClick={() => setSelectedId(s.id)} aria-pressed={s.id === selected.id}
          className="rounded-lg border border-indigo-200 px-3 py-2 text-sm text-indigo-800 aria-pressed:bg-indigo-50">
          {s.name} · {s.affectedDependentCount} affected services
        </button>)}
      </div> : <p className="mt-2 text-sm text-gray-500">No shared dependency meets the threshold.</p>}
      <div className="mt-5 border-t pt-4">
        <label htmlFor="impact-source" className="block text-sm font-medium mb-1">Trace an outage from</label>
        <select id="impact-source" className="border rounded-lg px-3 py-2 max-w-full" value={selected.id}
          onChange={e => setSelectedId(Number(e.target.value))}>
          {services.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        {selected.activeTickets.length > 0 && <div className="mt-3">
          <p className="text-xs font-medium text-gray-600 mb-1">Active incidents on {selected.name}</p>
          <TicketLinks tickets={selected.activeTickets} />
        </div>}
        <h4 className="mt-4 font-medium">Downstream impact paths</h4>
        {selected.dependents.length ? <ul className="mt-2 divide-y rounded-lg border px-3">
          {selected.dependents.map(d => <li key={d.id} className="py-3">
            <ol aria-label="Dependency path" className="flex flex-wrap items-center gap-2 text-sm mb-1">
              {d.path.map((name, index) => <li key={name} className="inline-flex items-center gap-2">
                {index > 0 && <ArrowRight size={14} aria-hidden="true" className="text-gray-500" />}
                <span className="rounded bg-gray-100 px-2 py-1">{name}</span>
              </li>)}
            </ol>
            {d.activeTickets.length ? <TicketLinks tickets={d.activeTickets} /> :
              <p className="text-xs text-gray-500">No active incidents on {d.path.at(-1)}</p>}
          </li>)}
        </ul> : <p className="mt-1 text-sm text-gray-500">No downstream services depend on {selected.name}.</p>}
      </div>
    </> : <p className="mt-3 text-sm text-gray-600">No services have been registered.</p>)}
  </section>;
}
