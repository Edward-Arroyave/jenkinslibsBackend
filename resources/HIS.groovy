return [
    AMBIENTES: [
        demo: [ BRANCH: "demo" ],
        test: [ BRANCH: "test" ]
    ],

    APIS: [
        API_CRTZ_WEB: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/API_CRTZ_WEB",
            CREDENCIALES: [
                demo: "",
                test: "PROFILE_HIS_API_CRTZ_WEB_TEST"
            ],
            URL: [
                demo: "",
                test: "https://his-backend-caracterizacion-annar-pruebas.azurewebsites.net"
            ]
        ],
        API_HIS: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/API_HIS",
            CREDENCIALES: [
                demo: "PROFILE_HIS_API_HIS_DEMO",
                test: "PROFILE_HIS_API_HIS_TEST"
            ],
            URL: [
                demo: "https://his-backend-annar-demo.azurewebsites.net",
                test: "https://his-backend-annar-pruebas.azurewebsites.net"
            ]
        ],
        API_HIS_APP: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/API_HIS_APP",
            CREDENCIALES: [
                demo: "PROFILE_HIS_API_HIS_APP_DEMO",
                test: "PROFILE_HIS_API_HIS_APP_TEST"
            ],
            URL: [
                demo: "https://his-backend-app-annar-demo-d7dfdve9c0bzc8fy.eastus2-01.azurewebsites.net",
                test: "https://his-backend-app-annar-pruebas-agejhqeqagebf2as.eastus2-01.azurewebsites.net"
            ]
        ],
        "Schedule.Api": [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/Schedule.Api",
            CREDENCIALES: [
                demo: "PROFILE_HIS_Schedule.Api_DEMO",
                test: "PROFILE_HIS_Schedule.Api_TEST"
            ],
            URL: [
                demo: "https://agendamiento-api-his-co-demo-eth4erhxh6d2g0fz.eastus2-01.azurewebsites.net/index.html",
                test: "https://agendamiento-api-his-co-pruebas-btc6csgbezbxgqep.eastus2-01.azurewebsites.net"
            ]
        ],
        ApiInteroperability: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/Schedule.Api",
            CREDENCIALES: [
                demo: "PROFILE_HIS_Schedule.Api_DEMO",
                test: "PROFILE_HIS_Schedule.Api_TEST"
            ],
            URL: [
                demo: "https://his-backend-interoperab-demo-fabhf0dgb8e4fng0.eastus2-01.azurewebsites.net",
                test: "https://his-backend-interoperab-pruebas-e7c0frh8d3fzamdx.eastus2-01.azurewebsites.net"
            ]
        ]
    ]
]
