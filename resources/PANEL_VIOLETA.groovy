return [
    AMBIENTES: [
        Test: [ BRANCH: "test" ],
        Pre_Produccion: [ BRANCH: "main" ]
    ],

    APIS: [
        PanelResultadosVioleta: [
            REPO_PATH: "${env.REPO_PATH}/PanelResultados",
            CREDENCIALES: [
                Test: "PROFILE_PANEL_VIOLETA_PanelResultados_TEST",
                Pre_Produccion: ""
            ],
            URL: [
                Test: "https://panel-violeta-backend-colcan-pruebas.azurewebsites.net",
                Pre_Produccion: ""
            ]
        ]
    ]
]
