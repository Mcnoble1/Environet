# EnviroNet Metagraph

## Overview

EnviroNet is a green energy participation metagraph designed to track and incentivize involvement in sustainable practices. The metagraph aims to reward individuals and businesses for their efforts in reducing carbon footprints, investing in green technologies, and using renewable energy sources. By integrating with various green energy platforms and data sources, EnviroNet encourages participation in eco-friendly initiatives and contributes to the broader goals of environmental sustainability.

## Metagraph Functionality

The EnviroNet metagraph periodically fetches data from multiple sources, applies minting logic based on the data, and rewards participants with tokens for their green energy activities.

## Data Sources and Workers

EnviroNet uses several data sources and workers/daemons, which are triggered periodically to check for new data and update the metagraph accordingly.

### Daemon/Worker Data Sources

1. **GreenEnergyPlatform Data:**
    - **Description:** Fetches data on energy consumption and usage of renewable sources from integrated green energy platforms.
    - **Worker Function:** The GreenEnergyPlatform worker retrieves data on user energy consumption and updates the metagraph with credits based on sustainable energy use.

2. **CarbonFootprintCalculator Data:**
    - **Description:** Gathers data on carbon footprint reductions reported by users through integrated carbon footprint calculators.
    - **Worker Function:** The CarbonFootprintCalculator worker processes data on carbon reductions and updates the metagraph with rewards for users who achieve significant reductions.

3. **InvestmentPlatform Data:**
    - **Description:** Monitors investments in green technologies and projects from integrated investment platforms.
    - **Worker Function:** The InvestmentPlatform worker checks for new investments and updates the metagraph with rewards based on the amount and impact of the investments.

4. **Manual Submissions:**
    - **Description:** Allows users to manually submit green energy-related data when automatic data fetching is unavailable.
    - **Worker Function:** The Manual Submissions worker processes manually submitted data and updates the metagraph accordingly.

### Manual Data Sources

1. **New Green Initiatives:**
    - **Description:** Rewards users for initiating and reporting new green energy projects or activities.
    - **Expected Update:** Send the following update to the metagraph for new green initiatives:

```json
{
  "value": {
    "NewGreenInitiative": {
      "user_id": ":user_id",
      "project_details": ":project_details"
    }
  },
  "proofs": [
    {
      "id": ":public_key",
      "signature": ":signature"
    }
  ]
}
```
NOTE: Only one update per initiative will be accepted. Duplicate submissions will be discarded.

### Token Minting Rates
- GreenEnergyPlatform Data: 20 tokens per verified report of renewable energy usage.
- CarbonFootprintCalculator Data: 30 tokens per verified carbon reduction milestone.
- InvestmentPlatform Data: 50 tokens per verified investment in green technologies.
- New Green Initiatives: 25 tokens per approved new green project or activity.

### Worker/Daemon Functionality
Each worker/daemon performs the following tasks on a defined schedule:

- Check for Updates: The worker queries the relevant data source for any new data or events related to green energy participation.
- Process Data: The worker processes the retrieved data to determine eligibility for rewards.
- Send Updates: The worker sends the processed data to the metagraph, updating it with new information and minting tokens as necessary.
This periodic checking mechanism ensures that the metagraph remains current with the latest green energy activities, accurately rewarding participants for their contributions.

### Conclusion
The EnviroNet Metagraph leverages a comprehensive set of data sources and workers to incentivize and track green energy participation. By continuously monitoring and updating the metagraph, EnviroNet ensures that positive environmental actions are recognized and rewarded, driving greater engagement in sustainability efforts.
