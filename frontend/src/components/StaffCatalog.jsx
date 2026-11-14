
import { useState } from 'react'

function StaffCatalog({
    products,
    status,
    onRetry,
    onLogout,
}) {
    const [selectedId, setSelectedId] = useState(null)

    const selectedProduct =
        products.find((p) => p.id === selectedId) ||
        products[0] ||
        null

    return (
        <main className="min-vh-100 bg-black text-light">
            <header className="border-bottom border-secondary py-3">
                <div className="container d-flex justify-content-between align-items-center">
                    <div className="d-flex align-items-center gap-3">
                        <img
                            src="/assets/logo.png"
                            alt="El Bajón del Vicho"
                            width="65"
                        />
                        <span className="fw-bold text-warning">
                            Panel VENDEDOR
                        </span>
                    </div>

                    <button
                        type="button"
                        className="btn btn-outline-light"
                        onClick={onLogout}
                    >
                        Cerrar sesión
                    </button>
                </div>
            </header>

            <div className="container py-5">
                <h1 className="h2 text-warning mb-3">
                    Catálogo de productos
                </h1>

                <p className="text-secondary mb-4">
                    Consulta los productos y sus detalles.
                    Este perfil no tiene permisos de edición.
                </p>

                {status === 'loading' && (
                    <p>Cargando productos...</p>
                )}

                {status === 'error' && (
                    <div role="alert">
                        <p>No se pudo cargar el catálogo.</p>
                        <button
                            type="button"
                            className="btn btn-warning"
                            onClick={onRetry}
                        >
                            Reintentar
                        </button>
                    </div>
                )}

                {status === 'success' && (
                    <div className="row g-4">
                        <div className="col-md-5">
                            <h2 className="h5 mb-3">
                                Lista de productos
                            </h2>

                            <div className="list-group">
                                {products.map((product) => (
                                    <button
                                        type="button"
                                        key={product.id}
                                        className={
                                            'list-group-item list-group-item-action ' +
                                            (selectedProduct?.id === product.id
                                                ? 'active'
                                                : '')
                                        }
                                        onClick={() =>
                                            setSelectedId(product.id)
                                        }
                                    >
                                        {product.name}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="col-md-7">
                            {selectedProduct && (
                                <article className="card bg-dark text-light border-secondary">
                                    <img
                                        src={selectedProduct.image}
                                        alt={selectedProduct.name}
                                        className="card-img-top"
                                        style={{
                                            maxHeight: '280px',
                                            objectFit: 'contain',
                                        }}
                                    />

                                    <div className="card-body">
                                        <h2 className="h4 text-warning">
                                            {selectedProduct.name}
                                        </h2>

                                        <p>{selectedProduct.description}</p>

                                        <p>
                                            <strong>Precio: </strong>
                                            ${Number(selectedProduct.price).toLocaleString('es-CL')}
                                        </p>

                                        <p>
                                            <strong>Categoría: </strong>
                                            {selectedProduct.category}
                                        </p>

                                        <p>
                                            <strong>Disponibilidad: </strong>
                                            {selectedProduct.stock
                                                ? 'Disponible'
                                                : 'Agotado'}
                                        </p>
                                    </div>
                                </article>
                            )}
                        </div>
                    </div>
                )}

                <section className="mt-5 border-top border-secondary pt-4">
                    <h2 className="h4 text-warning">
                        Órdenes
                    </h2>
                    <p className="text-secondary">
                        La consulta de órdenes se habilitará
                        cuando integremos el servicio de pedidos.
                    </p>
                </section>
            </div>
        </main>
    )
}

export default StaffCatalog
