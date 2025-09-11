return [
    AMBIENTES: [
        demo: [ BRANCH: "Demo" ],
        test: [ BRANCH: "Test" ]
    ],

    APIS: [
        Api_Agendamiento: [
            REPO_PATH: "${env.REPO_PATH}/Agendamiento/Api_Agendamiento",
            CREDENCIALES: [
                demo: "PROFILE_Api_Agendamiento_DEMO",
                test: "PROFILE_Api_Agendamiento_TEST"
            ],
            URL: [
                demo: "https://agendamiento-backend-co-demo-gba4fhbqexf2dzcc.eastus2-01.azurewebsites.net",
                test: "https://agendamiento-backend-co-pruebas-f9eqcgdtgkeug3cn.eastus2-01.azurewebsites.net"
            ],
            WebHook[
                value: "WEBHOOK_HEALTHBOOK", 
                chanel: "#desarrollos-backend"
            ]
        ],
        Api_ProcesosAuto: [
            REPO_PATH: "${env.REPO_PATH}/Agendamiento.ProcesosAuto/Api_ProcesosAuto",
            CREDENCIALES: [
                demo: "",
                test: "PROFILE_Api_ProcesosAuto_TEST"
            ],
            URL: [
                demo: "",
                test: "https://agendamiento-backend-ta-pruebas-cva3b9b3eubff3gf.eastus2-01.azurewebsites.net/"
            ]
        ]
    ]
]
