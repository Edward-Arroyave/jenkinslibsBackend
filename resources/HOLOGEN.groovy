return [
    AMBIENTES: [
        demo: [ BRANCH: "demo" ],
        test: [ BRANCH: "test" ]
    ],

    APIS: [
        Api_BioInformatica: [
            REPO_PATH: "${env.REPO_PATH}/API_Hologen/Api_BioInformatica",
            CREDENCIALES: [
                demo: "",
                test: "PROFILE_HIS_API_CRTZ_WEB_TEST"
            ],
            URL: [
                demo: "",
                test: "https://his-backend-caracterizacion-annar-pruebas.azurewebsites.net"
            ]
        ],
        API_Hologen: [
            REPO_PATH: "${env.REPO_PATH}/API_Hologen/API_HIS",
            CREDENCIALES: [
                demo: "PROFILE_HIS_API_HIS_DEMO",
                test: "PROFILE_HIS_API_HIS_TEST"
            ],
            URL: [
                demo: "https://his-backend-annar-demo.azurewebsites.net",
                test: "https://his-backend-annar-pruebas.azurewebsites.net"
            ]
        ],
        Api_Interoperabilidad: [
            REPO_PATH: "${env.REPO_PATH}/API_Hologen/API_HIS_APP",
            CREDENCIALES: [
                demo: "PROFILE_HIS_API_HIS_APP_DEMO",
                test: "PROFILE_HIS_API_HIS_APP_TEST"
            ],
            URL: [
                demo: "https://his-backend-app-annar-demo-d7dfdve9c0bzc8fy.eastus2-01.azurewebsites.net",
                test: "https://his-backend-app-annar-pruebas-agejhqeqagebf2as.eastus2-01.azurewebsites.net"
            ]
        ]
      
    ]
]
