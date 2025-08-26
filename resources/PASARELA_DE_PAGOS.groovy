return [
    AMBIENTES: [
        Test: [ BRANCH: "test" ],
        Pre_Produccion: [ BRANCH: "main" ]
    ],

    APIS: [
        Pasarela_Pagos: [
            REPO_PATH: "${env.REPO_PATH}/Pasarela_Pagos",
            CREDENCIALES: [
                Test: "PROFILE_PASARELA_DE_PAGOS_Pasarela_Pagos_TEST",
                Pre_Produccion: ""
            ],
            URL: [
                Test: "https://web-pagos-annar-back-pruebas-gtejcfhyeea7bkaz.eastus2-01.azurewebsites.net",
                Pre_Produccion: ""
            ]
        ]
    ]
]
