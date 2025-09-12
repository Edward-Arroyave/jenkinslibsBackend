return [
    AMBIENTES: [
        demo: [ BRANCH: "demo" ],
        test: [ BRANCH: "malla.dev" ]
    ],
    WEBHOOK_URL: '',
    APIS: [
        API_CRTZ_WEB: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/API_CRTZ_WEB",
            CREDENCIALES: [
                demo: "PROFILE_HIS_API_CRTZ_WEB_DEMO",
                test: "PROFILE_HIS_API_CRTZ_WEB_TEST"
            ],
            URL: [
                demo: "https://his-backend-caracterizacion-annar-demo.azurewebsites.net",
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
        ApiElectronicInvoice: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/ApiElectronicInvoice",
            CREDENCIALES: [
                demo: "PROFILE_HIS_ApiElectronicInvoice_DEMO",
                test: "PROFILE_HIS_ApiElectronicInvoice_TEST"
            ],
            URL: [
                demo: "https://his-backend-electronici-demo-f4epgjasdmcbaya9.eastus2-01.azurewebsites.net",
                test: "https://his-backend-electronici-pruebas-b0asd6dwf8ajexbp.eastus2-01.azurewebsites.net"
            ]
        ],
        "Financial.Api": [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/Financial.Api",
            CREDENCIALES: [
                demo: "PROFILE_HIS_Financial.Api_DEMO",
                test: "PROFILE_HIS_Financial.Api_TEST"
            ],
            URL: [
                demo: "https://his-backend-electronici-demo-f4epgjasdmcbaya9.eastus2-01.azurewebsites.net",
                test: "https://his-backend-fincanciera-pruebas-aeemf9fng6caa9gr.eastus2-01.azurewebsites.net"
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
            REPO_PATH: "${env.REPO_PATH}/API_HIS/ApiInteroperability",
            CREDENCIALES: [
                demo: "PROFILE_HIS_ApiInteroperability_DEMO",
                test: "PROFILE_HIS_ApiInteroperability_TEST"
            ],
            URL: [
                demo: "https://his-backend-interoperab-demo-fabhf0dgb8e4fng0.eastus2-01.azurewebsites.net",
                test: "https://his-backend-interoperab-pruebas-e7c0frh8d3fzamdx.eastus2-01.azurewebsites.net"
            ]
        ],
        "Invoice.Api": [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/Invoice.Api",
            CREDENCIALES: [
                demo: "PROFILE_HIS_Invoice.Api_DEMO",
                test: "PROFILE_HIS_Invoice.Api_TEST"
            ],
            URL: [
                demo: "https://his-backend-invoice-ann-demo-fhcgevbbgdfkc0cr.eastus2-01.azurewebsites.net",
                test: "https://his-backend-invoice-ann-pruebas-bpfbdyaybze7deb5.eastus2-01.azurewebsites.net"
            ]
        ],
        ApiPatient: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/ApiPatient",
            CREDENCIALES: [
                demo: "PROFILE_HIS_ApiPatient_DEMO",
                test: "PROFILE_HIS_ApiPatient_TEST"
            ],
            URL: [
                demo: "https://his-backend-pacientes-a-demo-bkgkexcdamgbb0a7.eastus2-01.azurewebsites.net",
                test: "https://his-backend-pacientes-a-test-azakd3d0g9ctavcj.eastus2-01.azurewebsites.net"
            ]
        ],
        ApiTask: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/ApiTask",
            CREDENCIALES: [
                demo: "PROFILE_HIS_ApiTaskt_DEMO",
                test: "PROFILE_HIS_ApiTask_TEST"
            ],
            URL: [
                demo: "https://his-backend-task-annar-demo-dmfeajbvd6ecebed.eastus2-01.azurewebsites.net",
                test: "https://his-backend-task-annar-pruebas-gzgdgsedevehbmff.eastus2-01.azurewebsites.net"
            ]
        ]
    ]
]
