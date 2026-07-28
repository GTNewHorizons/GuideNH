---
navigation:
  title: Mermaid Large Diagram
  position: 8150
---

TEST GOAL / 测试目标：20+ 节点大图，验证容器初始视口与页面高度行为

INVARIANTS / 不变式：初始视口合理、不无限撑高页面

## Large Flowchart (20+ Nodes)

Expected: All 23 nodes rendered in a single diagram; initial viewport shows a reasonable center-cropped portion; page height unaffected by diagram overflow (scroll within the Mermaid container).

```mermaid
flowchart TB
  classDef process fill:#7aa2f7,stroke:#2f3b54,color:#fff
  classDef data fill:#9ece6a,stroke:#2f3b54,color:#fff
  classDef decision fill:#e0af68,stroke:#2f3b54,color:#fff
  classDef term fill:#f7768e,stroke:#2f3b54,color:#fff

  Start([Start]):::term
  Load[Load Config]:::process
  Validate{Valid?}:::decision
  Parse[Parse Input]:::process
  Check{Has Data?}:::decision
  Fetch[Fetch Records]:::process
  Process[Process Each]:::process
  Filter[Apply Filter]:::process
  Sort[Sort Results]:::process
  Format[Format Output]:::process
  Render[Render View]:::process
  Error[Error Handler]:::term
  Retry[Retry]:::process
  Log[Log Error]:::process
  Notify[Notify Admin]:::process
  Cache[Cache Result]:::data
  Store[(Database)]:::data
  Backup[Backup]:::process
  Audit[Audit Trail]:::process
  Done(((Done))):::term
  Report[Generate Report]:::process
  Export[Export Data]:::process
  Cleanup[Cleanup]:::process

  Start --> Load
  Load --> Validate
  Validate -->|Yes| Parse
  Validate -->|No| Error
  Parse --> Check
  Check -->|Yes| Fetch
  Check -->|No| Error
  Fetch --> Process
  Process --> Filter
  Filter --> Sort
  Sort --> Format
  Format --> Render
  Render --> Done
  Error --> Retry
  Error --> Log
  Error --> Notify
  Retry --> Load
  Log --> Report
  Notify --> Report
  Done --> Cache
  Done --> Backup
  Done --> Audit
  Cache --> Store
  Backup --> Export
  Audit --> Cleanup
  Report --> Done
```

## Large Mindmap (20+ Nodes)

Expected: Mindmap with 21 nodes rendered; tree structure visible with root at center and alternating left/right subtrees.

```mermaid
mindmap
  root((Project))
    Planning
      Requirements
      Design
      Review
      Approval
    Development
      Frontend
        React
        CSS
        Tests
      Backend
        API
        Database
        Auth
      DevOps
        CI/CD
        Docker
        Monitoring
    QA
      Unit Tests
      Integration
      E2E
    Deploy
      Staging
      Production
      Rollback
```
