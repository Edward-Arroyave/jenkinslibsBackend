return [
    AMBIENTES: [
        Test: [ BRANCH: "test" ]
    ],
    WEBHOOK_URL: '',
    APIS: [
        ApiCrmVitalea: [
            REPO_PATH: "${env.REPO_PATH}/ApiCrmVitalea",
            CREDENCIALES: [
                Test: "PROFILE_CTS_VITALEA_ApiCrmVitalea_TEST",
                Pre_Produccion: ""
            ],
            URL: [
                Test: "http://crm-backend-pruebas.azurewebsites.net/swagger/ui/index",
                Pre_Produccion: ""
            ]
        ]
    ]
]
