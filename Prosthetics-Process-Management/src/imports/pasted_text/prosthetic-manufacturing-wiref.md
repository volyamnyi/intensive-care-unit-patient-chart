Create a complete low-fidelity wireframe prototype for a desktop web application used by prosthetists to manage and execute prosthetic manufacturing processes.
Use grayscale wireframes only.
Do not create visual branding or final UI design. Focus on information architecture, user journeys, navigation, layouts, workflow execution, process tracking, and data management.
The application is specifically designed for prosthetic manufacturing and prosthetic fitting workflows.
The system does not implement one fixed manufacturing process. Instead, it provides a configurable framework capable of executing different prosthetic production processes.
Each prosthetic process can contain:
•	Clinical stages
•	Technical production stages
•	Quality control checkpoints
•	Rework loops
•	Dynamic forms
•	Instructions from technological maps
•	Resource tracking
•	Material consumption tracking
The number of stages, steps, controls, checkpoints, and validation rules depends entirely on the selected process template.
The wireframes should represent the framework that executes configurable prosthetic workflows rather than a single predefined process.
________________________________________
Core User Journey
1.	User logs into the system.
2.	User creates a new manufacturing process.
3.	User selects a patient.
4.	User selects a prosthetic order.
5.	User reviews order information received through API.
6.	User selects a prosthetic process template.
7.	User executes the process through configurable stages and steps.
8.	System manages quality checkpoints and validations.
9.	User may pause and resume work.
10.	Process is either completed successfully or terminated as failed.
11.	Failed processes generate immutable Failure Snapshots.
12.	Completed and failed processes are archived.
________________________________________
Screen 1: Login
Components:
•	Application logo placeholder
•	Username field
•	Password field
•	Login button
Error State:
•	Invalid username/password message
•	Retry action
________________________________________
Screen 2: Dashboard
Purpose:
Primary workspace after authentication.
Header:
•	User profile
•	Notifications
•	Logout
Primary Actions:
•	New Process
•	Active Processes
•	Paused Processes
•	Completed Processes
•	Failed Processes
Main Table:
Columns:
•	Process ID
•	Patient
•	Prosthetic Order
•	Process Template
•	Current Stage
•	Current Step
•	Status
•	Last Updated
Statuses:
•	New
•	In Progress
•	Waiting for Review
•	Paused
•	Failed Quality Check
•	Failed
•	Completed
Display only processes assigned to the current user.
________________________________________
Screen 3: Patient Selection
Components:
•	Search field
•	Filters
•	Patient list
Columns:
•	Patient ID
•	Name
•	Date of Birth
•	Status
Actions:
•	Select Patient
________________________________________
Screen 4: Prosthetic Order Selection
Display all prosthetic orders associated with the selected patient.
Layout:
Patient Summary Panel
Order Table
Columns:
•	Order Number
•	Prosthesis Type
•	Prescription Date
•	Status
Actions:
•	Select Order
________________________________________
Screen 5: Order Review
Purpose:
Review external documentation received through API.
Layout:
Left Panel:
•	Patient information
•	Order information
Center Panel:
•	Document viewer
•	Scrollable document preview
Right Panel:
•	Metadata
•	Attachments
Actions:
•	Back
•	Main Menu
•	Continue
________________________________________
Screen 6: Process Template Selection
Purpose:
Select a prosthetic manufacturing process.
Examples:
•	Lower Limb Prosthesis
•	Upper Limb Prosthesis
•	Socket Fabrication
•	Refitting
•	Repair Process
Templates are configurable and loaded dynamically.
Each template card includes:
•	Process Name
•	Description
•	Estimated Duration
•	Number of Stages
Action:
•	Select Process
________________________________________
Screen 7: Process Overview
Purpose:
Visualize the technological map before execution.
Layout:
Left Sidebar:
Process Structure Tree
Display:
•	Clinical Stages
•	Production Stages
•	Quality Gates
Center Area:
Workflow Diagram
Display:
•	Stage sequence
•	Current stage
•	Completed stages
•	Upcoming stages
•	Quality checkpoints
Represent:
•	Stages as workflow nodes
•	Quality Gates as diamond nodes
•	Rework loops as return arrows
Use BPMN-inspired workflow visualization.
Right Sidebar:
Process Metadata
Display:
•	Process Name
•	Patient
•	Order
•	Assigned User
•	Status
•	Progress
Actions:
•	Start Process
•	Back
________________________________________
Screen 8: Process Execution Wizard
Purpose:
Execute technological instructions step-by-step.
Header:
•	Process Name
•	Patient
•	Order Number
Progress Area:
•	Overall Progress
•	Current Stage
•	Current Step
Main Content Area:
Display current instruction from the technological map.
Render dynamic controls based on process configuration.
Supported controls:
•	Checkbox
•	Text Input
•	Numeric Input
•	Dropdown
•	Radio Buttons
•	Text Area
•	Date Picker
•	File Upload
•	Image Upload
•	Signature Capture
Resource Tracking Section:
Allow recording:
•	Materials used
•	Material quantities
•	Components consumed
•	Labor time
•	Machine time
•	Notes
Actions:
•	Previous Step
•	Save Draft
•	Pause
•	Main Menu
•	Complete Step
Behavior:
Upon completion:
•	Validate data
•	Save entered values
•	Save resource consumption
•	Automatically move to next step
________________________________________
Screen 9: Quality Control Checkpoint
Purpose:
Validate a major process milestone.
Display:
•	Checkpoint Name
•	Description
•	Required Acceptance Criteria
Validation Area:
•	Checklist
•	Measurements
•	Notes
•	Attachments
Decision Actions:
•	Pass Checkpoint
•	Fail Checkpoint
If Passed:
•	Continue to next stage.
If Failed:
Show decision dialog:
•	Return for Rework
•	Mark Process as Failed
________________________________________
Screen 10: Rework Stage
Purpose:
Handle failed quality checkpoints that can be corrected.
Display:
•	Failed Checkpoint
•	Failure Reason
•	Required Corrective Actions
Actions:
•	Start Rework
•	View Previous Results
Behavior:
System routes the user back to the designated production stage.
________________________________________
Screen 11: Paused Processes
Table Columns:
•	Process ID
•	Patient
•	Prosthetic Order
•	Current Stage
•	Current Step
•	Pause Date
Actions:
•	Resume
•	View Details
________________________________________
Screen 12: Process History & Audit Trail
Purpose:
Display complete execution history.
Timeline Events:
•	Process Started
•	Stage Started
•	Stage Completed
•	Quality Check Passed
•	Quality Check Failed
•	Rework Started
•	Rework Completed
•	Pause
•	Resume
•	Process Failed
•	Process Completed
Filters:
•	User
•	Date
•	Event Type
________________________________________
Screen 13: Failed Process Handling
Purpose:
Manage processes that cannot continue.
A process may fail because of:
•	Manufacturing defect
•	Material issue
•	Repeated quality failures
•	Damaged component
•	Incorrect order
•	Patient rejection
•	Process cancellation
When a process is marked as Failed:
The system automatically creates an immutable Failure Snapshot.
Display:
•	Failure Reason
•	Failure Category
•	Failed Stage
•	Failed Step
•	Responsible User
•	Timestamp
Actions:
•	View Snapshot
•	Export Failure Report
•	Create Replacement Process
•	Return to Dashboard
________________________________________
Screen 14: Failure Snapshot
Purpose:
Preserve the complete state of the failed process.
Display:
Process Information:
•	Process ID
•	Process Template
•	Patient
•	Prosthetic Order
•	Failure Date
•	Failure Reason
Operational Data:
•	Completed Stages
•	Completed Steps
•	User Activities
•	Audit Trail
•	Notes
•	Attachments
Resource Consumption:
•	Materials Used
•	Material Quantities
•	Components Consumed
•	Labor Hours
•	Machine Time
Metrics:
•	Planned Duration
•	Actual Duration
•	Planned Material Usage
•	Actual Material Usage
•	Failure Point
Timeline:
Complete chronological event history.
Actions:
•	Export PDF Report
•	Print Snapshot
•	Return to Dashboard
________________________________________
Screen 15: Completed Process
Purpose:
Display successful process completion.
Sections:
•	Process Information
•	Patient Information
•	Prosthetic Order
•	Completed Stages
•	Quality Results
•	Attachments
•	Audit Trail
•	Final Outcome
Actions:
•	Export PDF
•	Return to Dashboard
________________________________________
Workflow Logic
Support:
•	Configurable prosthetic manufacturing processes
•	Clinical stages
•	Production stages
•	Dynamic stage structures
•	Dynamic forms
•	Quality checkpoints
•	Rework loops
•	Pause and resume
•	Resource tracking
•	Material consumption tracking
•	Labor tracking
•	Audit logging
•	Failure management
•	Failure snapshots
•	API-driven order documents
•	Automatic progression between steps
________________________________________
Navigation Flow
Login
→ Dashboard
→ New Process
→ Patient Selection
→ Prosthetic Order Selection
→ Order Review
→ Process Template Selection
→ Process Overview
→ Process Execution
→ Quality Control Checkpoint
→ Continue Process
→ Completed Process
Alternative Flow:
Process Execution
→ Pause
→ Paused Processes
→ Resume
→ Continue Process
Rework Flow:
Quality Control Checkpoint
→ Fail
→ Rework Stage
→ Return to Earlier Stage
→ Repeat Validation
→ Continue Process
Failure Flow:
Quality Control Checkpoint
→ Fail
→ Process Cannot Continue
→ Failed Process
→ Failure Snapshot
→ Archive
Generate all wireframes and connect them with clickable navigation and workflow transitions.
The resulting wireframe should clearly demonstrate how prosthetists manage configurable prosthetic manufacturing processes from order initiation through production, quality control, delivery, completion, or failure.

