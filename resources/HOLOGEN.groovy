return [
    AMBIENTES: [
        demo: [ BRANCH: "demo" ],
        test: [ BRANCH: "test" ]
    ],

    APIS: [
        Api_BioInformatica: [
            REPO_PATH: "${env.REPO_PATH}/API_Hologen/Api_BioInformatica",
            CREDENCIALES: [
                demo: "PROFILE_HOLOGEN_Api_BioInformatica_DEMO",
                test: "PROFILE_HOLOGEN_Api_BioInformatica_TEST"
            ],
            URL: [
                demo: "https://hologen-backend-colcan-cliente.azurewebsites.net",
                test: "https://bioinformatica-backend-colcan-pruebas.azurewebsites.net"
            ]
        ],
        API_Hologen: [
            REPO_PATH: "${env.REPO_PATH}/API_Hologen/API_Hologen",
            CREDENCIALES: [
                demo: "PROFILE_HOLOGEN_API_Hologen_DEMO",
                test: "PROFILE_HOLOGEN_API_Hologen_TEST"
            ],
            URL: [
                demo: "https://hologen-backend-colcan-cliente.azurewebsites.net",
                test: "https://hologen-backend-colcan-pruebas.azurewebsites.net"
            ]
        ],
        Api_Interoperabilidad: [
            REPO_PATH: "${env.REPO_PATH}/API_Hologen/Api_Interoperabilidad",
            CREDENCIALES: [
               demo: "PROFILE_HOLOGEN_Api_Interoperabilidad_DEMO",
                test: "PROFILE_HOLOGEN_Api_Interoperabilidad_TEST"
            ],
            URL: [
                demo: "https://hologen-backend-inter-colcan-demo.azurewebsites.net",
                test: "https://hologen-backend-inter-colcan-pruebas.azurewebsites.net"
            ]
        ]
      
    ]
]
