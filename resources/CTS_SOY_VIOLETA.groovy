return [
    AMBIENTES: [
        Test: [ BRANCH: "Test" ],
        Pre_Produccion: [ BRANCH: "main" ]
    ],
    WEBHOOK_URL: '',
    APIS: [
        ApiSoyVioleta: [
            REPO_PATH: "${env.REPO_PATH}/ApiCrmVitalea",
            CREDENCIALES: [
                Test: "PROFILE_CTS_VIOLETA_ApiSoyVioleta_TEST",
                Pre_Produccion: ""
            ],
            URL: [
                Test: "https://cts-back-soyvioleta-colcan-pruebas.azurewebsites.net",
                Pre_Produccion: ""
            ]
        ]
    ]
]
