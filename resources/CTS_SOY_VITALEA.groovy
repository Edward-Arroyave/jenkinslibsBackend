// APIS: [
//     ApiCrmVitalea: [
//         REPO_PATH: "${env.REPO_PATH}/ApiCrmVitalea",
//         LIBRARIES: [
//             VIEWMODELS_PATH: "${env.REPO_PATH}/ViewModels",
//             OUTPUT_DLL: "ApiCrmVitalea/bin/Release/ViewModels.dll"
//         ],
//         CREDENCIALES: [
//             test: "PROFILE_CTS_VIOLETA_ApiCrmVitalea_TEST",
//             demo: ""
//         ],
//         URL: [
//             test: "http://crm-backend-pruebas.azurewebsites.net",
//             demo: ""
//         ]
//     ]
// ]


return [
    AMBIENTES: [
        Test: [ BRANCH: "test" ]
    ],
    APIS: [
        ApiSoyVioleta: [
            REPO_PATH: "${env.REPO_PATH}/ApiCrmVitalea",
            CREDENCIALES: [
                Test: "PROFILE_CTS_VIOLETA_ApiCrmVitalea_TEST",
                Pre_Produccion: ""
            ],
            URL: [
                Test: "http://crm-backend-pruebas.azurewebsites.net",
                Pre_Produccion: ""
            ]
        ]
    ]
]
