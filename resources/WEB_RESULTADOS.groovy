return [
    AMBIENTES: [
        Colombia_Demo_RamaAimsaDemo: [ BRANCH: "aimsa.demo" ],
        Latam_Demo: [ BRANCH: "demo.LATAM" ],
        Aimsa_Demo: [ BRANCH: "aimsa.demo" ],
        Aimsa_Demo_RamaDemoLatam: [ BRANCH: "demo.LATAM" ]
    ],

    APIS: [
        Api_Web_Resultados: [
            REPO_PATH: "${env.REPO_PATH}/Api_Web_Resultados",
            CREDENCIALES: [
                Colombia_Demo_RamaAimsaDemo: "PROFILE_WEB_RESULT_Api_Web_Resultados_COLOMBIA_DEMO",
                Latam_Demo: "PROFILE_WEB_RESULT_Api_Web_Resultados_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_WEB_RESULT_Api_Web_Resultados_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_WEB_RESULT_Api_Web_Resultados_AIMSA_DEMO"
            ],
            URL: [
                Colombia_Demo_RamaAimsaDemo: "https://livelis-backend-resultados-annar-demo.azurewebsites.net",
                Latam_Demo: "https://livelis-webresultados-l-demo-e4bwbwg0bghna6ey.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-resultados-aims-demo-daajhebbfqc6a3aa.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-resultados-aims-demo-daajhebbfqc6a3aa.eastus2-01.azurewebsites.net"
            ]
        ]
    ]
]
