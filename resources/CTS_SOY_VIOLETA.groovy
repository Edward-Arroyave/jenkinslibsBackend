return [
    AMBIENTES: [
        Test: [ BRANCH: "Test" ],
        Pre_Produccion: [ BRANCH: "main" ]
    ],

    APIS: [
        ApiSoyVioleta: [
            REPO_PATH: "${env.REPO_PATH}/ApiSoyVioleta",
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
