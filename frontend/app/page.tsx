import { api } from '../lib/api';

export default async function Home() {
  let dashboard: any;

  try {
    dashboard = (await api('/dashboard')).data;
  } catch {
    dashboard = {
      customers: '—',
      orders: '—',
      revenue: '—',
      pendingOrders: '—',
      failedOrders: '—',
      lowStock: '—',
      recentOrders: [],
      statusDistribution: [],
    };
  }

  const cards = [
    ['Total customers', dashboard.customers],
    ['Orders processed', dashboard.orders],
    [
      'Revenue',
      typeof dashboard.revenue === 'number' ||
      typeof dashboard.revenue === 'string'
        ? `$${dashboard.revenue}`
        : '—',
    ],
    ['Pending orders', dashboard.pendingOrders],
    ['Failed orders', dashboard.failedOrders],
    ['Low stock products', dashboard.lowStock],
  ];

  return (
    <>
      <section className="title">
        <div>
          <h1>Operations dashboard</h1>
          <p>Live commercial activity and service health at a glance.</p>
        </div>
        <button>Export report</button>
      </section>

      <div className="cards">
        {cards.map(([label, value]) => (
          <article key={String(label)}>
            <small>{label}</small>
            <strong>{String(value)}</strong>
            <em>Live database metric</em>
          </article>
        ))}
      </div>

      <section className="grid">
        <article className="panel wide">
          <h2>Recent orders</h2>

          <table>
            <thead>
              <tr>
                <th>Order</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Total</th>
              </tr>
            </thead>

            <tbody>
              {dashboard.recentOrders.map((order: any) => (
                <tr key={order.id}>
                  <td>{order.order_number}</td>
                  <td>{order.customer}</td>
                  <td>
                    <span className="badge">{order.status}</span>
                  </td>
                  <td>${order.total_amount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </article>

        <article className="panel">
          <h2>Order status</h2>

          {dashboard.statusDistribution.map((status: any) => (
            <div className="bar" key={status.status}>
              <span>{status.status}</span>
              <b>{status.total}</b>
            </div>
          ))}
        </article>
      </section>
    </>
  );
}
