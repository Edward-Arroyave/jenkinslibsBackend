return [
    AMBIENTES: [
        Colombia_Test: [ BRANCH: "test" ],
        Colombia_Demo: [ BRANCH: "demo.lis" ],
        Colombia_PRE_PRODUCCION: [ BRANCH: "" ],
        Latam_Demo: [ BRANCH: "demo.LATAM" ],
        Aimsa_Demo: [ BRANCH: "aimsa.demo" ],
        Aimsa_Demo_RamaDemoLatam: [ BRANCH: "demo.LATAM" ],
        MX_Test: [ BRANCH: "colcan-mx.demo" ]
    ],

    APIS: [
        ApiANALYTICS: [
            REPO_PATH: "${env.REPO_PATH}/ApiANALYTICS",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiANALYTICS_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiANALYTICS_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiANALYTICS_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiANALYTICS_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiANALYTICS_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://livelis-backend-analitico-annar-pruebas.azurewebsites.net",
                Colombia_Demo: "https://livelis-backend-analitico-annar-demo.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-analitico-latam-demo-e2aaafadd9byfbcy.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-analitico-aimsa-demo-cjgmbtcmaghnhkf6.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-analitico-aimsa-demo-cjgmbtcmaghnhkf6.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        ApiAudit: [
            REPO_PATH: "${env.REPO_PATH}/ApiAudit",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiAudit_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiAudit_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiAudit_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiAudit_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiAudit_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://livelis-backend-auditoria-annar-pruebas.azurewebsites.net",
                Colombia_Demo: "https://livelis-backend-auditoria-annar-demo.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-auditoria-latam-demo-exdze9akgcafd0ff.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-auditoria-aimsa-demo-e4gsg5b6d4fmcjcw.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-auditoria-aimsa-demo-e4gsg5b6d4fmcjcw.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        ApiCovers: [
            REPO_PATH: "${env.REPO_PATH}/ApiCover",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiCovers_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiCovers_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiCovers_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiCovers_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiCovers_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://livelis-backend-covers--pruebas-f9dzh2fpa5h7e4aj.eastus2-01.azurewebsites.net",
                Colombia_Demo: "https://livelis-backend-covers--demo-epcadabmf6eacnhs.eastus2-01.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-covers-latam-demo-d8eteya7gxekbsen.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-covers-aimsa-demo-hre4efdzgyezcud2.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-covers-aimsa-demo-hre4efdzgyezcud2.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        APIInteroperabilidad: [
            REPO_PATH: "${env.REPO_PATH}/APIInteroperabilidad",
            CREDENCIALES: [
                Colombia_Test: "",
                Colombia_Demo: "",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_APIInteroperabilidad_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_APIInteroperabilidad_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_APIInteroperabilidad_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "",
                Colombia_Demo: "",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-interoperabilid-dem-ava7dyhbffamg6e7.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-inter-aimsa-demo-f7e0eud9hpfndpe7.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-inter-aimsa-demo-f7e0eud9hpfndpe7.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        ApiElectronicInvoice: [
            REPO_PATH: "${env.REPO_PATH}/ApiElectronicInvoice",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiElectronicInvoice_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiElectronicInvoice_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiElectronicInvoice_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiElectronicInvoice_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiElectronicInvoice_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://api-electronicinvoice-pruebas-a0frfze9gsbwfbhk.eastus2-01.azurewebsites.net",
                Colombia_Demo: "https://api-electronicinvoice-demo-bda9c0h9avfmavcs.eastus2-01.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-electronicinvoi-dem-drbhgwg3bqaycvgc.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-electronicinvoi-demo-bebjcwctesh7cegk.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-electronicinvoi-demo-bebjcwctesh7cegk.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        APINewPathology: [
            REPO_PATH: "${env.REPO_PATH}/APINewPathology",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_APINewPathology_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_APINewPathology_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_APINewPathology_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_APINewPathology_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_APINewPathology_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "http://patologia-backend-annar-pruebas.azurewebsites.net",
                Colombia_Demo: "http://patologia-backend-annar-demo.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-patologia-latam-demo-bycadff7afedcfbf.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-patologia-aimsa-demo-daejgshvh5czhmfn.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-patologia-aimsa-demo-daejgshvh5czhmfn.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        ApiPOSANALYTICS: [
            REPO_PATH: "${env.REPO_PATH}/ApiPOSANALYTICS",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiPOSANALYTICS_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiPOSANALYTICS_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiPOSANALYTICS_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiPOSANALYTICS_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiPOSANALYTICS_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://livelis-backend-posanalitico-annar-pruebas.azurewebsites.net",
                Colombia_Demo: "https://livelis-backend-posanalitico-annar-demo.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-posanalitico-la-demo-fze6ghava9bjgjdp.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-posanalitico-ai-demo-akfwaadnb0dwghhv.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-posanalitico-ai-demo-akfwaadnb0dwghhv.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        "ResultsExport.Api": [
            REPO_PATH: "${env.REPO_PATH}/ResultsExport.Api",
            CREDENCIALES: [
                Colombia_Test: "",
                Colombia_Demo: "PROFILE_LIS_ResultsExport.Api_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ResultsExport.Api_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ResultsExport.Api_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ResultsExport.Api_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "",
                Colombia_Demo: "https://livelis-backend-resulta-demo-ddaqceajc4hjcghx.eastus2-01.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-backend-resulta-de-eafva3f4hja3beac.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-backend-resulta-clie-g3gpg4avhjhygtfb.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-backend-resulta-clie-g3gpg4avhjhygtfb.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        "ResultsExport.Worker": [
            REPO_PATH: "${env.REPO_PATH}/ResultsExport.Worker",
            CREDENCIALES: [
                Colombia_Test: "",
                Colombia_Demo: "",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "",
                Aimsa_Demo: "",
                Aimsa_Demo_RamaDemoLatam: "",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "",
                Colombia_Demo: "",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "",
                Aimsa_Demo: "",
                Aimsa_Demo_RamaDemoLatam: "",
                MX_Test: ""
            ]
        ],
        ApiTaskHangfire: [
            REPO_PATH: "${env.REPO_PATH}/ApiTaskHangfire",
            CREDENCIALES: [
                Colombia_Test: "",
                Colombia_Demo: "PROFILE_LIS_ApiTaskHangfire_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "",
                Aimsa_Demo: "",
                Aimsa_Demo_RamaDemoLatam: "",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "",
                Colombia_Demo: "https://livelis-task-annar-demo.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "",
                Aimsa_Demo: "",
                Aimsa_Demo_RamaDemoLatam: "",
                MX_Test: ""
            ]
        ],
        ApiTrack: [
            REPO_PATH: "${env.REPO_PATH}/ApiTrack",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiTrack_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiTrack_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiTrack_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiTrack_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiTrack_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://livelis-backend-tracing-annar-pruebas.azurewebsites.net",
                Colombia_Demo: "https://livelis-backend-tracing-annar-demo.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-tracing-latam-demo-c3h3gsbxcvdmbbhp.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-tracing-aimsa-demo-bdhra6aydxdtc8ec.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-tracing-aimsa-demo-bdhra6aydxdtc8ec.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        ApiGlobalSettings: [
            REPO_PATH: "${env.REPO_PATH}/ApiGlobalSettings",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiGlobalSettings_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiGlobalSettings_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiGlobalSettings_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiGlobalSettings_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiGlobalSettings_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://livelis-backend-setting-pruebas-epgtdac5g0bnbceb.eastus2-01.azurewebsites.net",
                Colombia_Demo: "https://livelis-backend-setting-demo-bqchewctb6f8gqa9.eastus2-01.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-globalsettings--dem-cffbh9f8dtdpe2fj.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-globalsettings--demo-fsc2a5bhengrhyh6.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-globalsettings--demo-fsc2a5bhengrhyh6.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        IncomeResult: [
            REPO_PATH: "${env.REPO_PATH}/IncomeResult",
            CREDENCIALES: [
                Colombia_Test: "",
                Colombia_Demo: "PROFILE_LIS_IncomeResult_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_IncomeResult_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_IncomeResult_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_IncomeResult_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "",
                Colombia_Demo: "https://livelis-backend-analiti-demo-adhbegh3bkg2gzdn.eastus2-01.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "",
                Aimsa_Demo: "https://livelis-insertresult-ai-demo-exhhhedhadbkhdat.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-insertresult-ai-demo-exhhhedhadbkhdat.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        ApiInventory: [
            REPO_PATH: "${env.REPO_PATH}/ApiInventory",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiInventory_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiInventory_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiInventory_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiInventory_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiInventory_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "https://livelis-backend-inventa-pruebas-a2bgedfnabe9frad.eastus2-01.azurewebsites.net",
                Colombia_Demo: "https://livelis-backend-inventa-demo-dyeaepa4fecdfyex.eastus2-01.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-inventarios-lat-demo-aceed6gvged3ckbh.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-inventarios-aim-demo-hrcuc3hycye5dvb8.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-inventarios-aim-demo-hrcuc3hycye5dvb8.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ],
        ApiLIS: [
            REPO_PATH: "${env.REPO_PATH}/ApiLIS",
            CREDENCIALES: [
                Colombia_Test: "PROFILE_LIS_ApiLIS_COLOMBIA_TEST",
                Colombia_Demo: "PROFILE_LIS_ApiLIS_COLOMBIA_DEMO",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "PROFILE_LIS_ApiLIS_LATAM_DEMO",
                Aimsa_Demo: "PROFILE_LIS_ApiLIS_AIMSA_DEMO",
                Aimsa_Demo_RamaDemoLatam: "PROFILE_LIS_ApiLIS_AIMSA_DEMO",
                MX_Test: ""
            ],
            URL: [
                Colombia_Test: "http://livelis-backend-annar-pruebas.azurewebsites.net",
                Colombia_Demo: "http://livelis-backend-annar-demo.azurewebsites.net",
                Colombia_PRE_PRODUCCION: "",
                Latam_Demo: "https://livelis-preanalitico-la-demo-dufjfse9hbc9dmbq.eastus2-01.azurewebsites.net",
                Aimsa_Demo: "https://livelis-preanalitico-ai-demo-gtayfnhydzhha6h9.eastus2-01.azurewebsites.net",
                Aimsa_Demo_RamaDemoLatam: "https://livelis-preanalitico-ai-demo-gtayfnhydzhha6h9.eastus2-01.azurewebsites.net",
                MX_Test: ""
            ]
        ]
    ]
]
