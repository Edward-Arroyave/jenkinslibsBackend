return [
    AMBIENTES: [
        demo: [ BRANCH: "demo" ],
        test: [ BRANCH: "Test" ]
    ],

    APIS: [
        backendQC: [
            REPO_PATH: "${env.REPO_PATH}/qc_backend_web",
            CREDENCIALES: [
                demo: "",
                test: "QC_BACKEND_TEST"
            ],
            URL: [
                demo: "",
                test: "https://his-backend-caracterizacion-annar-pruebas.azurewebsites.net"
            ]
        ],
    ]
]
