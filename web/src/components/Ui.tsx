import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react'
import { AlertCircle, CheckCircle2, LoaderCircle, X } from 'lucide-react'

export function Button({
  children,
  className = '',
  loading,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { loading?: boolean }) {
  return (
    <button className={`button ${className}`} disabled={loading || props.disabled} {...props}>
      {loading && <LoaderCircle className="spin" size={17} />}
      {children}
    </button>
  )
}

export function PageTitle({ eyebrow, title, children, action }: { eyebrow?: string; title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <header className="page-title">
      <div>{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h1>{title}</h1>{children && <p>{children}</p>}</div>
      {action}
    </header>
  )
}

export function EmptyState({ icon, title, children, action }: { icon: ReactNode; title: string; children: ReactNode; action?: ReactNode }) {
  return <div className="empty-state"><span className="empty-icon">{icon}</span><h3>{title}</h3><p>{children}</p>{action}</div>
}

export function Toast({ type = 'success', children, onClose }: { type?: 'success' | 'error'; children: ReactNode; onClose?: () => void }) {
  return <div className={`toast ${type}`} role="status">{type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}<span>{children}</span>{onClose && <button onClick={onClose} aria-label="Close"><X size={16} /></button>}</div>
}

export function Skeleton({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={`skeleton ${className}`} {...props} />
}
