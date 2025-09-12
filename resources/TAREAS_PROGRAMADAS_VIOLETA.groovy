return [
    AMBIENTES: [
        Test: [ BRANCH: "test" ],
        Pre_Produccion: [ BRANCH: "main" ]
    ],
    WEBHOOK_URL: '',
    APIS: [
        ScheduledTasksVioleta: [
            REPO_PATH: "${env.REPO_PATH}/ScheduledTasks",
            CREDENCIALES: [
                Test: "PROFILE_TAREAS_PROG_VIOLETA_ScheduledTasksVioleta_TEST",
                Pre_Produccion: ""
            ],
            URL: [
                Test: "https://task-soyvioleta-back-colcan-pruebas.azurewebsites.net",
                Pre_Produccion: ""
            ]
        ]
    ]
]
