return [
    demo: [ CS_PROJ_PATH: "${env.REPO_PATH}/API_HIS/API_HIS", CREDENTIALS_ID: "CredencialesDemoApi1",BRANCH: "demo" ],
    test: [ CS_PROJ_PATH: "${env.REPO_PATH}/API_HIS/Schedule.Api", CREDENTIALS_ID: "PROFILE_HIS_API_HIS_TEST",BRANCH:"test", ],
]
