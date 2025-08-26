return [
    AMBIENTES: [
        Test: [ BRANCH: "test" ],
        Pre_Produccion: [ BRANCH: "main" ]
    ],

    APIS: [
        PanelResultados: [
            REPO_PATH: "${env.REPO_PATH}/PanelResultados",
            CREDENCIALES: [
                Test: "PROFILE_PANEL_VITALEA_PanelResultados_TEST",
                Pre_Produccion: ""
            ],
            URL: [
                Test: "https://panel-r-backend-vitalea-pruebas.azurewebsites.net",
                Pre_Produccion: ""
            ]
        ]
    ]
]
