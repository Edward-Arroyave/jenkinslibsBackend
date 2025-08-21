return [
    AMBIENTES: [
        demo: [ BRANCH: "demo" ],
        test: [ BRANCH: "Test" ]
    ],

    APIS: [
        backendQC: [
            REPO_PATH: "${env.REPO_PATH}/qc_backend_web",
            CREDENCIALES: [
                demo: "QC_BACKEND_DEMO",
                test: "QC_BACKEND_TEST"
            ],
            URL: [
                demo: "https://valiqc-backend-general-demo.azurewebsites.net",
                test: "https://valiqc-backend-general-pruebas.azurewebsites.net"
            ]
        ]
    ]
]
