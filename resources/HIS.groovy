return [
    // PRIMER NIVEL: Ambientes y ramas comunes
    AMBIENTES: [
        demo: [
            BRANCH: "demo"
        ],
        test: [
            BRANCH: "test"
        ]
    ],

    // SEGUNDO NIVEL: APIs
    APIS: [
        API_HIS: [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/API_HIS",
            CREDENCIALES: [
                demo: "CredencialesDemoApi1",
                test: "PROFILE_HIS_API_HIS_TEST"
            ]
        ],
        "Schedule.Api": [
            REPO_PATH: "${env.REPO_PATH}/API_HIS/Schedule.Api",
            CREDENCIALES: [
                demo: "PROFILE_HIS_Schedule.Api_DEMO",
                test: "PROFILE_HIS_Schedule.Api_TEST"
            ]
        ]
    ]
]
