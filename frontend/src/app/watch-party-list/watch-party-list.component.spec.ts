import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WatchPartyListComponent } from './watch-party-list.component';

describe('WatchPartyListComponent', () => {
  let component: WatchPartyListComponent;
  let fixture: ComponentFixture<WatchPartyListComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [WatchPartyListComponent]
    });
    fixture = TestBed.createComponent(WatchPartyListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
