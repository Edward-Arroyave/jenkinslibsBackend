return [
    AMBIENTES: [
        Test: [ BRANCH: "Test" ]
    ],
    WEBHOOK_URL: '',
    APIS: [
        WebApiVitaleaApp: [
            REPO_PATH: "${env.REPO_PATH}/Codigo/WebApiVitaleaApp",
            CREDENCIALES: [
                Test: "PROFILE_APP_VITALEA_WebApiVitaleaApp_TEST"
            ],
            URL: [
                Test: "https://app-backend-vitalea-desarrollo.azurewebsites.net"
            ]
        ]
    ]
]
