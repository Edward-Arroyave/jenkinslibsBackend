return [
    AMBIENTES: [
        test: [ BRANCH: "test" ],
        demo: [ BRANCH: "demo" ]
    ],

    APIS: [
        ApiCrmVitalea: [
            REPO_PATH: "${env.REPO_PATH}",
            CREDENCIALES: [
                test: "PROFILE_CTS_VIOLETA_ApiCrmVitalea_TEST",
                demo: ""
            ],
            URL: [
                test: "http://crm-backend-pruebas.azurewebsites.net",
                demo: ""
            ]
        ]
    ]
]
