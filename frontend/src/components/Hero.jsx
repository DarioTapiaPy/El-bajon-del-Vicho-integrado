
function Hero() {
    return (
        <section id="inicio" className="bg-black py-5">
            <div className="container py-lg-5">
                <div className="row align-items-center">

                    {/* TEXTO PRINCIPAL */}
                    <div className="col-12 col-lg-6 text-center text-lg-start">

                        <p className="text-warning fw-bold text-uppercase mb-3">
                            HAMBURGUESAS · DELIVERY · RESERVAS
                        </p>

                        <h1
                            className="fw-bold text-warning lh-1"
                            style={{
                                fontSize: 'clamp(2.4rem, 8.5vw, 5rem)',
                                overflowWrap: 'break-word'
                            }}
                        >
                            EL BAJÓN
                            <br />
                            DEL VICHO
                        </h1>

                        <p className="lead text-light my-4">
                            Hamburguesas monstruosas para esos ataques
                            de hambre que no perdonan.
                        </p>

                        {/* BOTONES */}
                        <div className="d-flex flex-column flex-sm-row gap-3 justify-content-center justify-content-lg-start">

                            <a
                                href="#menu"
                                className="btn btn-warning fw-bold px-4 py-3"
                            >
                                Ver menú
                            </a>

                            <a
                                href="#reservas"
                                className="btn btn-outline-light fw-bold px-4 py-3"
                            >
                                Reservar mesa
                            </a>

                        </div>
                    </div>

                    {/* IMAGEN RESPONSIVA */}
                    <div
                        className="col-12 col-lg-6 text-center mt-4 mt-lg-0"
                        style={{
                            minWidth: 0,
                            maxWidth: '100%',
                            overflow: 'hidden'
                        }}
                    >
                        <img
                            src="/assets/logo.png"
                            alt="Hamburguesa El Bajón del Vicho"
                            className="img-fluid"
                            style={{
                                display: 'block',
                                width: '100%',
                                maxWidth: 'min(78vw, 500px)',
                                height: 'auto',
                                objectFit: 'contain',
                                marginLeft: 'auto',
                                marginRight: 'auto'
                            }}
                        />
                    </div>

                </div>
            </div>
        </section>
    );
}

export default Hero;
