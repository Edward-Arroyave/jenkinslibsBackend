return [
    AMBIENTES: [
        demo: [ BRANCH: "Demo" ],
        test: [ BRANCH: "Test" ]
    ],

    APIS: [
        Api_Agendamiento: [
            REPO_PATH: "${env.REPO_PATH}/qc_backend_web",
            CREDENCIALES: [
                demo: "QC_BACKEND_DEMO",
                test: "QC_BACKEND_TEST"
            ],
            URL: [
                demo: "https://agendamiento-backend-co-demo-gba4fhbqexf2dzcc.eastus2-01.azurewebsites.net",
                test: "https://agendamiento-backend-co-pruebas-f9eqcgdtgkeug3cn.eastus2-01.azurewebsites.net"
            ]
        ],
        Api_ProcesosAuto: [
            REPO_PATH: "${env.REPO_PATH}/qc_backend_web",
            CREDENCIALES: [
                demo: "",
                test: ""
            ],
            URL: [
                demo: "",
                test: ""
            ]
        ]
    ]
]
