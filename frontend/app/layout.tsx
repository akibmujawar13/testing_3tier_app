import './styles.css';
import Link from 'next/link';
import FrontendObservability from '../components/frontend-observability';

export const metadata = {
  title: 'Enterprise Order & Fulfillment',
  description: 'Observability commerce platform',
};

export default function Layout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <FrontendObservability />

        <aside>
          <div className="brand">
            NORTHSTAR<span>OPS</span>
          </div>

          <p>FULFILLMENT PLATFORM</p>

          <nav>
            <Link href="/">Dashboard</Link>
            <Link href="/products">Product Catalog</Link>
            <Link href="/customers">Customers</Link>
            <Link href="/orders">Order Management</Link>
            <Link href="/operations">Operations</Link>
          </nav>

          <div className="env">
            ● DEV ENVIRONMENT
            <br />
            <small>APM ready</small>
          </div>
        </aside>

        <main>
          <header>
            <div>
              <b>Enterprise Operations</b>
              <small>Observability Commerce Platform</small>
            </div>

            <div className="live">● Systems operational</div>
          </header>

          {children}
        </main>
      </body>
    </html>
  );
}
