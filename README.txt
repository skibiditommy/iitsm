================================================================================
I-ITSM CAPSTONE PROJECT - HANDOVER INSTRUCTIONS
Course: CS F213 / MAC F212 (Object-Oriented Programming)
================================================================================

HOW TO RESUME ON YOUR MACHINE:

1. Unzip this folder to your Desktop or preferred workspace.
2. Open Antigravity (or your IDE with the Agent).
3. Set the workspace root to this 'iitsm' folder.
4. Open the file 'HANDOVER_PROMPT.txt' in this directory.
5. Copy the ENTIRE text of 'HANDOVER_PROMPT.txt' and paste it into the agent chat.
6. The agent will read all 72 existing Java files, acknowledge the architecture, and continue building from Step 1 (Factories, Repositories, DTOs, Adapters, Services, Frontend, Tests, and Docs).

FILES INCLUDED IN THIS PACKAGE:
- backend/pom.xml & docker-compose.yml
- backend/src/main/resources/ (application.yml with H2 config, application-docker.yml)
- backend/src/main/java/com/iitsm/
    ├── IitsmApplication.java
    ├── domain/enums/ (11 files: Role, Status, Priority, Impact, Urgency, etc.)
    ├── domain/valueobject/ (2 files: PriorityScore, SlaTarget)
    ├── domain/exception/ (15 files: IitsmException hierarchy)
    ├── domain/model/ (12 JPA entities: User hierarchy, Incident, ITService, etc.)
    ├── domain/state/ (11 files: 8 State pattern classes + Factory + Interface)
    ├── domain/strategy/ (8 files: 2 Priority + 4 Assignment strategies)
    └── domain/observer/ (13 files: IncidentEvent, 10 event records, Publisher)
- docs/00_PHASE_1_IMPLEMENTATION_PLAN.md (Full approved architecture document)
================================================================================
